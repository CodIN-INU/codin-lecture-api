package inu.codin.codin.domain.review.exception;

import inu.codin.codin.global.common.exception.GlobalException;
import lombok.Getter;

@Getter
public class ReviewException extends GlobalException {
    private final ReviewErrorCode errorCode;

    /**
     * Constructs a new ReviewException with the specified review error code.
     *
     * @param errorCode the specific error code representing the review-related error
     */
    public ReviewException(ReviewErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
