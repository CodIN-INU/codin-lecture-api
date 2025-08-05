package inu.codin.codin.domain.like.controller;

import inu.codin.codin.domain.like.dto.LikeRequestDto;
import inu.codin.codin.domain.like.dto.LikeResponseType;
import inu.codin.codin.domain.like.dto.LikeType;
import inu.codin.codin.domain.like.dto.LikedResponseDto;
import inu.codin.codin.domain.like.service.LikeService;
import inu.codin.codin.global.common.response.SingleResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeFeignClient likeFeignClient;
    private final LikeService likeService;

    @Operation(summary = "LECTURE(과목), REVIEW(수강 후기) 에 대한 좋아요 기능",
            description = "LikeType = LECTURE, REVIEW <br>" +
                    "id = 좋아요를 누를 entity의 pk"
    )
    @PostMapping
    public ResponseEntity<SingleResponse<?>> toggleLike(@RequestBody @Valid LikeRequestDto likeRequestDto) {
        LikeResponseType message = likeService.toggleLike(likeRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SingleResponse<>(201, "좋아요가 " + message.getDescription() + "되었습니다.", message.toString()));
    }

    /**
     * Retrieves the total number of likes for a specified entity type and ID.
     *
     * @param likeType the type of entity to count likes for
     * @param id the unique identifier of the entity
     * @return the total like count for the specified entity
     */
    @Hidden
    @GetMapping
    public Integer getLikeCount(
            @RequestParam("likeType") LikeType likeType,
            @RequestParam("id") String id) {
        return likeFeignClient.getLikeCount(likeType, id);
    }

    /**
     * Determines whether the current user has liked the specified entity.
     *
     * @param likeType the type of entity to check (e.g., lecture, review)
     * @param id the unique identifier of the entity
     * @return {@code true} if the user has liked the entity; {@code false} otherwise
     */
    @Hidden
    @GetMapping("/user")
    public Boolean isUserLiked(
            @RequestParam("likeType") LikeType likeType,
            @RequestParam("id") String id) {
        return likeService.isLiked(likeType, id);
    }

    @Hidden
    @GetMapping("/list")
    public List<LikedResponseDto> getLiked(@RequestParam("likeType") LikeType likeType) {
        return likeService.getLiked(likeType);
    }
}
