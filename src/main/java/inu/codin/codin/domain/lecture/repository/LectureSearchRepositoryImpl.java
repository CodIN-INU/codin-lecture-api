package inu.codin.codin.domain.lecture.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inu.codin.codin.domain.lecture.entity.*;
import inu.codin.codin.domain.lecture.exception.LectureErrorCode;
import inu.codin.codin.domain.lecture.exception.LectureException;
import inu.codin.codin.domain.like.controller.LikeFeignClient;
import inu.codin.codin.domain.like.dto.LikeType;
import inu.codin.codin.global.auth.util.SecurityUtils;
import inu.codin.codin.global.common.entity.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LectureSearchRepositoryImpl implements LectureSearchRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Lecture> searchLecturesAtPreview(String keyword, Department department, SortingOption sortingOption, List<Long> liked, Pageable pageable) {
        QLecture lecture = QLecture.lecture;
        QTag tag = QTag.tag;
        QLectureTag lectureTag = QLectureTag.lectureTag;

        BooleanBuilder builder = new BooleanBuilder();

        searchByDepartment(department, builder, lecture); //학과 조건 추가
        searchByKeyword(keyword, builder, lecture, lectureTag); //키워드 조건 추가
        addUserLikedFilter(liked, builder, lecture); //좋아요 조건 추가

        JPQLQuery<Lecture> query = jpaQueryFactory
                .selectDistinct(lecture)
                .from(lecture);

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(lecture.countDistinct())
                .from(lecture);

        if (keyword != null && !keyword.isBlank()) { //키워드가 존재한다면 tags에 대하여 left Join 진행
            applyTagJoins(query, lecture, lectureTag, tag);
            applyTagJoins(countQuery, lecture, lectureTag, tag);
        }

        //조건에 맞는 강의들에 대해서만 필터링
        query.where(builder)
                .orderBy(getOrderSpecifier(lecture, sortingOption)) //정렬 조건 추가
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Lecture> lectures = query.fetch();

        Long total = countQuery
                .where(builder)
                .fetchOne();

        return new PageImpl<>(lectures, pageable, total != null? total : 0);
    }

    private void applyTagJoins(JPQLQuery<?> query, QLecture lecture, QLectureTag lectureTag, QTag tag) {
        query.leftJoin(lecture.tags, lectureTag)
                .leftJoin(lectureTag.tag, tag);
    }

    private void addUserLikedFilter(List<Long> liked, BooleanBuilder builder, QLecture lecture) {
        if (liked != null && !liked.isEmpty()) {
            builder.and(lecture.id.in(liked));
        }
    }


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

    private void searchByDepartment(Department department, BooleanBuilder builder, QLecture lecture) {
        if (department != null) { //학과에 대한 정렬이 필요하다면 검증 후 조건 추가
            validDepartment(department);
            builder.and(lecture.department.eq(department));
        }
    }

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

    private void validDepartment(Department department) {
        if (! (department.equals(Department.EMBEDDED) || department.equals(Department.COMPUTER_SCI) || department.equals(Department.INFO_COMM)))
            throw new LectureException(LectureErrorCode.DEPARTMENT_WRONG_INPUT);
    }
}
