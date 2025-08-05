package inu.codin.codin.domain.elasticsearch.service;


import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import inu.codin.codin.domain.elasticsearch.document.LectureDocument;
import inu.codin.codin.domain.lecture.entity.SortingOption;
import inu.codin.codin.global.common.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureElasticService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Searches for lecture document IDs in Elasticsearch using full-text search, optional department and liked lecture filters, and sorting.
     *
     * Performs a multi-field full-text search on lecture documents based on the provided keyword, with optional filtering by department and a list of liked lecture IDs. Results are sorted according to the specified sorting option (rating, likes, or hits), and paginated.
     *
     * @param keyword    The search keyword for full-text matching. If blank or null, all lectures are matched.
     * @param department Optional filter to restrict results to a specific department.
     * @param sortOption Sorting criteria: rating, likes, or hits. If null, defaults to rating.
     * @param likedIds   Optional list of lecture IDs to filter results to those liked by the user.
     * @param pageNumber Zero-based index of the results page to retrieve.
     * @param size       Number of results per page.
     * @return A paginated list of lecture IDs matching the search and filter criteria.
     */
    public Page<Long> searchIds(
            String keyword,
            Department department,
            SortingOption sortOption,
            List<Long> likedIds,
            int pageNumber,
            int size
    ) {
        log.info("강의 검색 - keyword={}, Department={}, sortingOption={}, likedIdsCount={}, pageNumber={}, size={}", keyword, department, sortOption, likedIds != null ? likedIds.size() : 0, pageNumber, size);

        Query boolQuery = QueryBuilders.bool(b -> {
            if (keyword != null && !keyword.isBlank()) {
                b.should(s -> s.multiMatch(mm -> mm
                                .fields("lectureNm^3", "tags^2", "professor", "type", "lectureType", "evaluation", "preCourses")
                                .query(keyword)
                                .operator(Operator.Or)
                        ))
                        .should(s -> s.wildcard(w -> w
                                .field("lectureNm")
                                .value("*" + keyword + "*")
                        ))
                        .should(s -> s.match(m -> m
                                .field("tags")
                                .query(keyword)
                        ))
                        .minimumShouldMatch("1"); // 최소 매칭 갯수
            } else {
                // 검색 키워드가 없으면 모든 문서 반환
                b.must(m -> m.matchAll(ma -> ma));
            }

            // 소속 별 검색
            if (department != null) {
                b.filter(f -> f.term(t -> t.field("department").value(department.name())));
            }
            // 좋아요 id
            if (likedIds != null && !likedIds.isEmpty()) {
                b.filter(f -> f.ids(i -> i.values(likedIds.stream().map(String::valueOf).toList())));
            }
            return b;
        });

        // 정렬 기준 옵션
        List<SortOptions> sortOptions = List.of(
                getSortOption(sortOption),
                SortOptions.of(o -> o.field(f -> f.field("starRating").order(SortOrder.Desc))),
                SortOptions.of(o -> o.field(f -> f.field("likes").order(SortOrder.Desc)))
        );

        // ES NativeQuery 생성
        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery)
                .withSort(sortOptions)
                .withPageable(PageRequest.of(pageNumber, size))
                .build();

        // 검색 전 쿼리 로깅
        SearchHits<LectureDocument> hits = elasticsearchOperations.search(query, LectureDocument.class, IndexCoordinates.of("lectures"));
        log.info("Search returned {} hits out of {} total", hits.getSearchHits().size(), hits.getTotalHits());

        List<Long> ids = hits.getSearchHits()
                .stream()
                .map(hit -> hit.getContent().getId())
                .toList();

        return new PageImpl<>(ids,
                PageRequest.of(pageNumber, size),
                hits.getTotalHits());
    }

    /**
     * Returns the Elasticsearch sort option corresponding to the specified sorting criterion.
     *
     * If the sorting option is null or set to RATING, sorts by descending star rating.
     * If set to LIKE, sorts by descending number of likes.
     * If set to HIT, sorts by descending number of hits.
     *
     * @param sortOption the sorting criterion to apply
     * @return the Elasticsearch sort option for the given criterion
     */
    private SortOptions getSortOption(SortingOption sortOption) {
        if (sortOption == null) {
            return SortOptions.of(o -> o.field(f -> f.field("starRating").order(SortOrder.Desc)));
        }

        return switch (sortOption) {
            case RATING -> SortOptions.of(o -> o.field(f -> f.field("starRating").order(SortOrder.Desc)));
            case LIKE   -> SortOptions.of(o -> o.field(f -> f.field("likes").order(SortOrder.Desc)));
            case HIT    -> SortOptions.of(o -> o.field(f -> f.field("hits").order(SortOrder.Desc)));
        };
    }
}

