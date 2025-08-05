package inu.codin.codin.domain.elasticsearch.service;

import inu.codin.codin.domain.elasticsearch.convertor.LectureDocumentConverter;
import inu.codin.codin.domain.elasticsearch.document.LectureDocument;
import inu.codin.codin.domain.elasticsearch.event.LectureDeletedEvent;
import inu.codin.codin.domain.elasticsearch.event.LectureSavedEvent;
import inu.codin.codin.domain.elasticsearch.repository.LectureElasticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class LectureIndexService {

    private final LectureElasticRepository lectureElasticRepository;

    private final LectureDocumentConverter lectureDocumentConverter;

    // todo: 또는 엔티티 콜백 인터페이스 구현 방법도 있음
    // BeforeConvertCallback, AfterSaveCallback, AfterDeleteCallback...

    /**
     * Handles a lecture save or update event by synchronizing the corresponding lecture data with the Elasticsearch index.
     *
     * @param event the event containing the saved or updated lecture entity
     */
    @TransactionalEventListener(classes = {LectureSavedEvent.class})
    public void handleLectureSaved(LectureSavedEvent event) {
        LectureDocument document = lectureDocumentConverter.convertToDocument(event.getLecture());
        lectureElasticRepository.save(document);
    }

    /**
     * Handles the deletion of a lecture by removing its corresponding document from the Elasticsearch index.
     *
     * @param event the event containing the ID of the deleted lecture
     */
    @TransactionalEventListener(classes = {LectureDeletedEvent.class})
    public void handleLectureDeleted(LectureDeletedEvent event) {
        lectureElasticRepository.deleteById(event.lectureId());
    }
}
