package inu.codin.codin.domain.lecture.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inu.codin.codin.domain.elasticsearch.service.LectureElasticService;
import inu.codin.codin.domain.lecture.entity.*;
import inu.codin.codin.domain.lecture.exception.LectureErrorCode;
import inu.codin.codin.domain.lecture.exception.LectureException;
import inu.codin.codin.global.common.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LectureSearchRepositoryImpl implements LectureSearchRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    private final LectureElasticService lectureElasticService;
    private final LectureRepository lectureRepository;

    /**
     * Searches for lectures matching the given keyword, department, sorting option, and liked lecture IDs, returning a paginated result.
     *
     * This method uses Elasticsearch to retrieve lecture IDs based on the search criteria and pagination, then fetches the corresponding Lecture entities from the database, preserving the order from the search results.
     *
     * @param keyword        the search keyword to filter lectures
     * @param department     the department to filter lectures by
     * @param sortingOption  the sorting option to apply to the search results
     * @param liked          a list of lecture IDs that the user has liked, used to influence search results
     * @param pageable       pagination information for the result set
     * @return a page of lectures matching the search criteria, ordered as in the search results
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Lecture> searchLecturesAtPreview(String keyword, Department department, SortingOption sortingOption, List<Long> liked, Pageable pageable) {
        log.debug("[searchLecturesAtPreview] 강의 조회, keyword={}, department={}, sortingOption={}, liked={}", keyword, department, sortingOption, liked);
        Page<Long> idPage = lectureElasticService.searchIds(keyword, department, sortingOption, liked, pageable.getPageNumber(), pageable.getPageSize());

        List<Long> ids = idPage.getContent();
        long total = idPage.getTotalElements();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // JPA로 Lecture 엔티티 Fetch 조회
        Page<Lecture> lectures = lectureRepository.findAllWithAssociationsByIds(ids);
        log.info("[searchLecturesAtPreview] 강의 조회, size={} ", lectures.getTotalElements());

        // ES 정렬 순서 유지하며 재정렬
        Map<Long, Lecture> lectureMap = lectures.stream()
                .collect(Collectors.toMap(Lecture::getId, Function.identity()));
        List<Lecture> ordered = ids.stream()
                .map(lectureMap::get)
                .filter(Objects::nonNull)
                .toList();

        // Page<Lecture> 변환하여 반환
        return new PageImpl<>(ordered, pageable, total);
    }

    /****
     * Adds left joins for lecture tags and tags to the provided QueryDSL JPQL query.
     *
     * @param query the QueryDSL JPQL query to modify
     * @param lecture the QLecture entity path
     * @param lectureTag the QLectureTag entity path
     * @param tag the QTag entity path
     */
    @Deprecated
    private void applyTagJoins(JPQLQuery<?> query, QLecture lecture, QLectureTag lectureTag, QTag tag) {
        query.leftJoin(lecture.tags, lectureTag)
                .leftJoin(lectureTag.tag, tag);
    }

    /**
     * Adds a filter to the query to include only lectures whose IDs are present in the provided liked list.
     *
     * @param liked the list of lecture IDs that are liked
     * @param builder the BooleanBuilder to which the filter condition is added
     * @param lecture the QueryDSL lecture entity used for building the filter
     */
    @Deprecated
    private void addUserLikedFilter(List<Long> liked, BooleanBuilder builder, QLecture lecture) {
        if (liked != null && !liked.isEmpty()) {
            builder.and(lecture.id.in(liked));
        }
    }

    /**
     * Adds OR conditions to the provided BooleanBuilder to filter lectures by keyword across multiple fields, including lecture name, type, evaluation, pre-course, professor, and associated tag names.
     *
     * @param keyword the search keyword to match against lecture fields and tags
     * @param builder the BooleanBuilder to which the conditions are added
     * @param lecture the QueryDSL QLecture entity reference
     * @param lectureTag the QueryDSL QLectureTag entity reference
     */
    @Deprecated
    private void searchByKeyword(String keyword, BooleanBuilder builder, QLecture lecture, QLectureTag lectureTag) {
        if (keyword != null && !keyword.isBlank()){ //키워드가 있다면 모든 정보에 대해서 확인
            builder.andAnyOf(
                    lecture.lectureNm.containsIgnoreCase(keyword),
                    lecture.lectureType.containsIgnoreCase(keyword),
                    lecture.evaluation.stringValue().containsIgnoreCase(keyword),
                    lecture.preCourse.containsIgnoreCase(keyword),
                    lecture.type.stringValue().containsIgnoreCase(keyword),
                    lecture.professor.containsIgnoreCase(keyword),
                    lectureTag.tag.tagName.containsIgnoreCase(keyword)
                    //todo 이후에 추가될 수 있는 필드들에 대해서도 조건 추가
            );
        }
    }

    /**
     * Returns an array of QueryDSL {@code OrderSpecifier} objects for sorting lectures based on the given sorting option.
     *
     * If {@code sortingOption} is null, the default order is by star rating, likes, and hits in descending order.
     *
     * @param lecture the QueryDSL QLecture instance used for specifying sort fields
     * @param sortingOption the sorting criteria (HIT, LIKE, RATING), or null for default sorting
     * @return an array of {@code OrderSpecifier} objects representing the sort order
     */
    @Deprecated
    private OrderSpecifier[] getOrderSpecifier(QLecture lecture, SortingOption sortingOption) {
        if (sortingOption == null) {
            return new OrderSpecifier[]{lecture.starRating.desc(),
                    lecture.likes.desc(),
                    lecture.hits.desc()}; // 기본 정렬
        }

        return switch (sortingOption) {
            case HIT -> new OrderSpecifier[]{
                    lecture.hits.desc(),
                    lecture.starRating.desc(),
                    lecture.likes.desc()
            };
            case LIKE -> new OrderSpecifier[]{
                    lecture.likes.desc(),
                    lecture.starRating.desc(),
                    lecture.hits.desc()
            };
            case RATING -> new OrderSpecifier[]{
                    lecture.starRating.desc(),
                    lecture.likes.desc(),
                    lecture.hits.desc()
            };
        };
    }

    /**
     * Retrieves a list of lectures filtered by department, and optionally by grade and semester.
     *
     * If a grade is provided, only lectures matching that grade are included. If a semester is provided, lectures are joined with their semesters and filtered accordingly.
     *
     * @param department the department to filter lectures by; must be valid
     * @param grade the grade to filter lectures by, or null to include all grades
     * @param semester the semester to filter lectures by, or null to include all semesters
     * @return a list of lectures matching the specified criteria
     */
    @Override
    public List<Lecture> searchLecturesAtReview(Department department, Integer grade, Semester semester) {
        QLecture lecture = QLecture.lecture;

        BooleanBuilder builder = new BooleanBuilder();
        searchByDepartment(department, builder, lecture);
        if (grade != null) { //학년이 있다면 조건 추가
            builder.and(lecture.grade.eq(grade));
        }

        JPQLQuery<Lecture> query = jpaQueryFactory
                .selectFrom(lecture);

        if (semester != null) { //학기가 있다면 semester에 대한 join 진행
            query.leftJoin(lecture.semester).fetchJoin();
            builder.and(lecture.semester.any().semester.eq(semester));
        }

        return query
                .where(builder)
                .fetch();
    }

    /**
     * Adds a department filter to the query builder after validating the department.
     *
     * If the department is not null, validates it and appends a condition to filter lectures by the specified department.
     *
     * @param department the department to filter by; if null, no filter is applied
     * @param builder the BooleanBuilder to which the filter condition is added
     * @param lecture the QueryDSL lecture entity used for building the condition
     */
    private void searchByDepartment(Department department, BooleanBuilder builder, QLecture lecture) {
        if (department != null) { //학과에 대한 정렬이 필요하다면 검증 후 조건 추가
            validDepartment(department);
            builder.and(lecture.department.eq(department));
        }
    }

    /**
     * Validates that the provided department is one of the allowed departments (EMBEDDED, COMPUTER_SCI, or INFO_COMM).
     *
     * @param department the department to validate
     * @throws LectureException if the department is not valid
     */
    private void validDepartment(Department department) {
        if (! (department.equals(Department.EMBEDDED) || department.equals(Department.COMPUTER_SCI) || department.equals(Department.INFO_COMM)))
            throw new LectureException(LectureErrorCode.DEPARTMENT_WRONG_INPUT);
    }
}
