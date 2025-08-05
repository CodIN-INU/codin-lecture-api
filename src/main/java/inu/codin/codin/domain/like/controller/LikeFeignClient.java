package inu.codin.codin.domain.like.controller;


import inu.codin.codin.domain.like.dto.LikeRequestDto;
import inu.codin.codin.domain.like.dto.LikeType;
import inu.codin.codin.domain.like.dto.LikedResponseDto;
import inu.codin.codin.global.common.response.SingleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "likeClient", url = "${server.feign.url}")
public interface LikeFeignClient {

    /**
     * Toggles the like status for a given entity based on the provided request data.
     *
     * @param likeRequestDto the request data specifying the entity and user for the like action
     * @return a response entity containing the result of the toggle operation
     */
    @PostMapping
    ResponseEntity<SingleResponse<?>> toggleLike(@RequestBody LikeRequestDto likeRequestDto);

    /**
     * Retrieves the number of likes for a specified entity and like type.
     *
     * @param likeType the type of entity being liked
     * @param id the identifier of the entity
     * @return the total count of likes for the given entity and type
     */
    @GetMapping
    Integer getLikeCount(
            @RequestParam("likeType") LikeType likeType,
            @RequestParam("id") String id
    );

    /**
     * Checks whether a specific user has liked a given entity of the specified type.
     *
     * @param likeType the type of entity being liked
     * @param id the identifier of the entity
     * @param userId the identifier of the user
     * @return true if the user has liked the entity; false otherwise
     */
    @GetMapping("/user")
    Boolean isUserLiked(
            @RequestParam("likeType") LikeType likeType,
            @RequestParam("id") String id,
            @RequestParam("userId") String userId
    );

    /**
     * Retrieves a list of entities liked by a specific user for the given like type.
     *
     * @param likeType the type of entity for which likes are being queried
     * @param userId the identifier of the user whose liked entities are to be retrieved
     * @return a list of responses representing entities liked by the user
     */
    @GetMapping("/list")
    List<LikedResponseDto> getLiked(
            @RequestParam("likeType") LikeType likeType,
            @RequestParam("userId") String userId
    );
}
