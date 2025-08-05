package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.lecture.dto.LectureDetailResponseDto;
import inu.codin.codin.domain.lecture.dto.LecturePageResponse;
import inu.codin.codin.domain.lecture.dto.LecturePreviewResponseDto;
import inu.codin.codin.domain.lecture.dto.LectureSearchListResponseDto;
import inu.codin.codin.domain.lecture.entity.Emotion;
import inu.codin.codin.domain.lecture.entity.Lecture;
import inu.codin.codin.domain.lecture.entity.Semester;
import inu.codin.codin.domain.lecture.entity.SortingOption;
import inu.codin.codin.domain.lecture.exception.LectureErrorCode;
import inu.codin.codin.domain.lecture.exception.LectureException;
import inu.codin.codin.domain.lecture.exception.SemesterErrorCode;
import inu.codin.codin.domain.lecture.exception.SemesterException;
import inu.codin.codin.domain.lecture.repository.LectureRepository;
import inu.codin.codin.domain.lecture.repository.LectureSearchRepositoryCustom;
import inu.codin.codin.domain.like.dto.LikeType;
import inu.codin.codin.domain.like.service.LikeService;
import inu.codin.codin.domain.review.service.UserReviewStatsService;
import inu.codin.codin.global.common.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LectureService {

    private final LectureRepository lectureRepository;
    private final LectureSearchRepositoryCustom lectureSearchRepository;

    private final EmotionService emotionService;
    private final SemesterService semesterService;
    private final UserReviewStatsService userReviewStatsService;
    private final LikeService likeService;

    /****
     * Returns a paginated list of lectures filtered and sorted by keyword, department, sorting option, and liked status.
     *
     * @param keyword        the search keyword to filter lectures
     * @param department     the department to filter by, or null to include all departments
     * @param sortingOption  the sorting criteria (e.g., relevance, rating count, likes, views)
     * @param like           if true, filters to lectures liked by the current user
     * @param page           the page number to retrieve
     * @return a paginated response containing lecture previews and pagination details
     */
    public LecturePageResponse sortListOfLectures(String keyword, Department department, SortingOption sortingOption, Boolean like, int page) {
        // 조회 유저의 좋아요 목록을 조회해 반환
        List<Long> liked = null;
        if (like != null && like) {
            liked = likeService.getLiked(LikeType.LECTURE).stream()
                    .map(likedResponseDto -> Long.valueOf(likedResponseDto.getLikeTypeId()))
                    .toList();
        }

        Page<Lecture> lecturePage = lectureSearchRepository.searchLecturesAtPreview(keyword, department, sortingOption, liked, PageRequest.of(page, 10));
        return getLecturePageResponse(lecturePage);
    }

    /**
     * 강의의 상세 별점 반환
     * @param lectureId 강의 id
     * @return LectureDetailResponseDto
     */
    @Transactional
    public LectureDetailResponseDto getLectureDetails(Long lectureId) {
        Lecture lecture = lectureRepository.findLectureWithScheduleAndTagsById(lectureId)
                .orElseThrow(() -> new LectureException(LectureErrorCode.LECTURE_NOT_FOUND));
        lecture.increaseHits();
        Emotion emotion = emotionService.getOrMakeEmotion(lecture);
        return LectureDetailResponseDto.of(lecture, emotion, userReviewStatsService.isOpenKeyword());
    }

    /**
     * Converts a paginated list of Lecture entities into a LecturePageResponse DTO, including like status for each lecture.
     *
     * @param lecturePage the paginated Lecture entities to convert
     * @return a LecturePageResponse containing lecture previews, total pages, and the next page number
     */
    private LecturePageResponse getLecturePageResponse(Page<Lecture> lecturePage) {
        return LecturePageResponse.of(lecturePage.stream()
                        .map(lecture -> {
                            boolean liked = false;
                            try {
                                liked = likeService.isLiked(LikeType.LECTURE, lecture.getId().toString());
                            } catch (Exception e) {
                                // 좋아요 상태 확인 실패 시 false로 처리
                                log.warn("Failed to check like status for lecture {}: {}", lecture.getId(), e.getMessage());
                            }
                            return LecturePreviewResponseDto.of(lecture, liked);
                        }).toList(),
                lecturePage.getTotalPages() - 1,
                lecturePage.hasNext() ? lecturePage.getPageable().getPageNumber() + 1 : -1);
    }

    /**
     * Searches for lectures eligible for review writing based on department, grade, and semester.
     *
     * @param department the department to filter lectures by, or null for all departments
     * @param grade the student grade to filter lectures by
     * @param semester the semester string (e.g., "23-1"); if null, searches across all semesters
     * @return a list of lectures matching the criteria, formatted for review writing
     * @throws SemesterException if the specified semester does not exist
     */
    public List<LectureSearchListResponseDto> searchLecturesToReview(Department department, Integer grade, String semester) {
        Semester semesterEntity = (semester != null) ? semesterService.getSemester(semester).orElseThrow(() -> new SemesterException(SemesterErrorCode.SEMESTER_NOT_FOUND)) : null;
        List<Lecture> lectures = lectureSearchRepository.searchLecturesAtReview(department, grade, semesterEntity);

        return (semesterEntity != null)
                ? lectures.stream()
                        .map(lecture -> LectureSearchListResponseDto.of(lecture, semester))
                        .toList()
                : lectures.stream()
                        .map(LectureSearchListResponseDto::of)
                        .flatMap(List::stream)
                        .toList();
    }

}
