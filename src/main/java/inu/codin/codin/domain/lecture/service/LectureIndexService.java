package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.lecture.converter.LectureToDocumentConverter;
import inu.codin.codin.domain.lecture.search.document.LectureDocument;
import inu.codin.codin.domain.lecture.event.LectureDeletedEvent;
import inu.codin.codin.domain.lecture.event.LectureSavedEvent;
import inu.codin.codin.domain.lecture.repository.elasticsearch.LectureElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureIndexService {

    private final LectureElasticRepository lectureElasticRepository;
    private final LectureToDocumentConverter lectureToDocumentConverter;

    public void incrementHit(Long lectureId) {
        try {
            lectureElasticRepository.incrementHit(lectureId);
        } catch (IOException e) {
            log.error("조회수 업데이트 실패 lectureId={}, {}", lectureId, e.getMessage());
        }
    }

    public void updateStarRating(Long lectureId, Double newRating) {
        try {
            lectureElasticRepository.updateStarRating(lectureId, newRating);
        } catch (IOException e) {
            log.error("별점 업데이트 실패. lectureId={}, newRating={}, {}", lectureId, newRating, e.getMessage());
        }
    }

    public void incrementLike(Long lectureId) {
        try {
            lectureElasticRepository.incrementLike(lectureId);
        } catch (IOException e) {
            log.error("좋아요 업데이트 실패 lectureId={}, {}", lectureId, e.getMessage());
        }
    }

    public void decrementLike(Long lectureId) {
        try {
            lectureElasticRepository.decrementLike(lectureId);
        } catch (IOException e) {
            log.error("좋아요 업데이트 실패 lectureId={}, {}", lectureId, e.getMessage());
        }
    }

    // todo: 또는 엔티티 콜백 인터페이스 구현 방법도 있음
    // BeforeConvertCallback, AfterSaveCallback, AfterDeleteCallback...

    /**
     * 강의 저장 / 수정 후 호출
     * ApplicationEventPublisher -> LectureSavedEvent
     * @param event LectureSavedEvent
     */
    @TransactionalEventListener(classes = {LectureSavedEvent.class})
    public void handleLectureSaved(LectureSavedEvent event) {
        LectureDocument document = lectureToDocumentConverter.convertToDocument(event.lecture());
        try {
            lectureElasticRepository.saveLecture(document);
        } catch (IOException e) {
            log.error("강의 저장 실패, {}" , e.getMessage());
        }
    }

    /**
     * 강의 삭제 시 호출
     * ApplicationEventPublisher -> LectureDeletedEvent
     * @param event LectureDeletedEvent
     */
    @TransactionalEventListener(classes = {LectureDeletedEvent.class})
    public void handleLectureDeleted(LectureDeletedEvent event) {
        try {
            lectureElasticRepository.deleteLecture(event.lectureId());
        } catch (IOException e) {
            log.error("강의 삭제 실패, {}" , e.getMessage());
        }
    }
}
