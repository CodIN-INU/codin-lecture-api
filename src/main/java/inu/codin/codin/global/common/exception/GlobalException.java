package inu.codin.codin.global.common.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException{

    private final GlobalErrorCode errorCode;

    public GlobalException(GlobalErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }
}
