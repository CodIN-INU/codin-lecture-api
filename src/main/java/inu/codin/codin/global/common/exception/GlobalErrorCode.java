package inu.codin.codin.global.common.exception;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public interface GlobalErrorCode {
    HttpStatus httpStatus();
    String message();
    Level logEvent();
}