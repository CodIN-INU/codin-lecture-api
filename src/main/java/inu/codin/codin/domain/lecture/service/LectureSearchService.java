package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.lecture.document.LectureDocument;
import inu.codin.codin.domain.lecture.dto.LecturePageResponse;
import inu.codin.codin.domain.lecture.dto.LecturePreviewResponseDto;
import inu.codin.codin.domain.lecture.entity.SortingOption;
import inu.codin.codin.domain.lecture.repository.elasticsearch.LectureElasticRepository;
import inu.codin.codin.domain.like.dto.LikeType;
import inu.codin.codin.domain.like.service.LikeService;
import inu.codin.codin.global.common.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureSearchService {
    private static final int PAGE_SIZE = 10;

    private final LikeService likeService;
    private final LectureElasticRepository lectureElasticRepository;

    /**
     * ElasticSearch 강의 검색 메서드
     *
     * @param keyword    키워드 (풀텍스트 검색)
     * @param department 학과 필터 (null 가능)
     * @param sortOption 정렬 옵션 (별점, 좋아요, 조회수)
     * @param pageNumber 0-based 페이지 번호
     * @return Page<Long>
     */
    public LecturePageResponse searchLectureDocumentList(String keyword, Department department, SortingOption sortOption, int pageNumber, Boolean like) {
        try {
            Map<Long, Boolean> likeMap = likeService.getLiked(LikeType.LECTURE).stream()
                    .collect(Collectors.toMap(
                            likedDto -> Long.valueOf(likedDto.getLikeTypeId()),
                            likedDto -> true
                    ));

            List<String> likeIdList = getLikeIdList(likeMap);
            Page<LectureDocument> lectureDocumentPage = lectureElasticRepository.searchLectureDocumentList(keyword, department, sortOption, likeIdList, pageNumber, PAGE_SIZE, like);

            return getLecturePageResponse(lectureDocumentPage, likeMap);
        } catch (IOException e) {
            log.error("검색에 실패했습니다. : {}", e.getMessage());

            return LecturePageResponse.of(new ArrayList<>(), 0, 0);
        }
    }

    private List<String> getLikeIdList(Map<Long, Boolean> likeMap) {
        return likeMap.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(String::valueOf)
                .toList();
    }

    /**
     * 페이지로 반환된 LectureEntity -> Dto 변환
     */
    private LecturePageResponse getLecturePageResponse(Page<LectureDocument> lecturePage, Map<Long, Boolean> likeMap) {

        return LecturePageResponse.of(lecturePage.stream()
                        .map(lectureDocument -> {
                                    int likeCount = likeService.getLikeCount(LikeType.LECTURE, String.valueOf(lectureDocument.getId()));

                                    return LecturePreviewResponseDto.of(lectureDocument, likeCount, likeMap.getOrDefault(lectureDocument.getId(), false));
                                }
                        )
                        .toList(),
                lecturePage.getTotalPages() - 1,
                lecturePage.hasNext() ? lecturePage.getPageable().getPageNumber() + 1 : -1);
    }
}
