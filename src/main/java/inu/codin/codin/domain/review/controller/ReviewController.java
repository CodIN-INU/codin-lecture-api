package inu.codin.codin.domain.review.controller;

import inu.codin.codin.domain.review.dto.CreateReviewRequestDto;
import inu.codin.codin.domain.review.service.ReviewService;
import inu.codin.codin.global.common.response.SingleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Review API", description = "수강 후기 API")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Creates a new review for the specified lecture.
     *
     * Handles POST requests to create a review associated with a given lecture ID using the provided review data.
     * Returns a response indicating successful creation without a response body payload.
     *
     * @param lectureId the ID of the lecture to associate the review with
     * @param createReviewRequestDto the review creation request data
     * @return HTTP 201 Created response with a completion message and no data payload
     */
    @Operation(
            summary = "수강 후기 작성"
    )
    @PostMapping("/{lectureId}")
    public ResponseEntity<SingleResponse<?>> createReview(
            @PathVariable("lectureId") Long lectureId,
            @RequestBody @Valid CreateReviewRequestDto createReviewRequestDto) {
        reviewService.createReview(lectureId, createReviewRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SingleResponse<>(201, "수강 후기 작성 완료", null));
    }

    /**
     * Retrieves a paginated list of reviews for the specified lecture.
     *
     * @param lectureId the ID of the lecture for which to fetch reviews
     * @param page the page number to retrieve
     * @return a response entity containing a standardized response with the list of reviews
     */
    @Operation(
            summary = "해당 강의의 수강 후기 반환"
    )
    @GetMapping("/{lectureId}")
    public ResponseEntity<SingleResponse<?>> getListOfReviews(
            @PathVariable("lectureId") Long lectureId,
            @RequestParam("page") int page) {
        return ResponseEntity.ok()
                .body(new SingleResponse<>(200, "수강 후기 리스트 반환", reviewService.getListOfReviews(lectureId, page)));
    }
}
