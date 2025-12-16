package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.lecture.dto.LectureDetailResponseDto;
import inu.codin.codin.domain.lecture.dto.LectureSearchListResponseDto;
import inu.codin.codin.domain.lecture.entity.Emotion;
import inu.codin.codin.domain.lecture.entity.Lecture;
import inu.codin.codin.domain.lecture.entity.Semester;
import inu.codin.codin.domain.lecture.exception.LectureErrorCode;
import inu.codin.codin.domain.lecture.exception.LectureException;
import inu.codin.codin.domain.lecture.exception.SemesterErrorCode;
import inu.codin.codin.domain.lecture.exception.SemesterException;
import inu.codin.codin.domain.lecture.repository.jpa.LectureRepository;
import inu.codin.codin.domain.lecture.repository.jpa.LectureSearchRepositoryCustom;
import inu.codin.codin.domain.review.service.UserReviewStatsService;
import inu.codin.codin.global.common.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final LectureIndexService lectureIndexService;
    private final UserReviewStatsService userReviewStatsService;

    /**
     * 강의의 상세 별점 반환
     *
     * @param lectureId 강의 id
     * @return LectureDetailResponseDto
     */
    @Transactional
    public LectureDetailResponseDto getLectureDetails(Long lectureId) {
        Lecture lecture = lectureRepository.findLectureWithScheduleAndTagsById(lectureId)
                .orElseThrow(() -> new LectureException(LectureErrorCode.LECTURE_NOT_FOUND));
        lecture.increaseHits();

        Emotion emotion = emotionService.getOrMakeEmotion(lecture);

        lectureIndexService.incrementHit(lectureId);

        return LectureDetailResponseDto.of(lecture, emotion, userReviewStatsService.isOpenKeyword());
    }

    /**
     * 강의 후기를 작성할 강의 목록 검색
     *
     * @param department Department (COMPUTER_SCI, INFO_COMM, EMBEDDED)
     * @param grade      학년 (1,2,3,4)
     * @param semester   수강 학기 (23-1, 23-2,,, 현재 학기)
     * @return List<LectureSearchListResponseDto> 검색 결과 리스트 반환
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
