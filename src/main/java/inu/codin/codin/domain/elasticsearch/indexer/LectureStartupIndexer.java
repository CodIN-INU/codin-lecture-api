package inu.codin.codin.domain.elasticsearch.indexer;

import inu.codin.codin.domain.elasticsearch.convertor.LectureDocumentConverter;
import inu.codin.codin.domain.elasticsearch.document.LectureDocument;
import inu.codin.codin.domain.elasticsearch.repository.LectureElasticRepository;
import inu.codin.codin.domain.lecture.entity.Lecture;
import inu.codin.codin.domain.lecture.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LectureStartupIndexer {

    private final LectureElasticRepository lectureElasticRepository;
    private final LectureRepository lectureRepository;

    private final LectureDocumentConverter lectureDocumentConverter;

    private final int CHUNK_SIZE = 100;

    @Value("${elasticsearch.indexer.enabled:true}")
    private boolean indexerEnabled;

    /****
     * Indexes all lecture data into Elasticsearch upon application startup if enabled.
     *
     * This method listens for the application ready event and, if the indexer is enabled via configuration,
     * deletes any existing lecture documents in the Elasticsearch index and reindexes all lectures from the database
     * in batches. Each lecture is converted to a document and saved to Elasticsearch. Progress and results are logged.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 환경 변수로 설정 적용
        if (!indexerEnabled) {
            log.info("ElasticSearch indexer 사용할 수 없습니다.");
            return;
        }

        long existingCount = lectureElasticRepository.count();
        if (existingCount > 0) {
            log.info("ElasticSearch 인덱스가 이미 존재합니다 ({}개), 전체 재인덱싱을 진행합니다.", existingCount);
            // 선택적으로 기존 인덱스 삭제 후 재인덱싱
            lectureElasticRepository.deleteAll();
            log.info("기존 인덱스를 삭제했습니다.");
        }

        log.info("Starting ElasticSearch Indexing, Lecture Data");
        Pageable pageable = PageRequest.of(0, CHUNK_SIZE);
        Page<Lecture> page;
        int totalProcessed = 0;

        do {
            page = lectureRepository.findAllPaged(pageable);
            List<LectureDocument> documents = page.getContent().stream()
                    .map(lectureDocumentConverter::convertToDocument)
                    .toList();

            lectureElasticRepository.saveAll(documents);
            totalProcessed += documents.size();

            log.info("[indexAllLectures] Indexed Lecture: {}, Total: {})", documents.size(), totalProcessed);
            pageable = page.nextPageable();
        } while (page.hasNext());

        log.info("ElasticSearch 인덱싱 성공. Total Indexed: {}", totalProcessed);
    }


}
