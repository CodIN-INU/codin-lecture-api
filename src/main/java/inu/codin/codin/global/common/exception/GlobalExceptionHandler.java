package inu.codin.codin.global.common.exception;

import inu.codin.codin.global.common.response.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ExceptionResponse> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
    }

    @ExceptionHandler(GlobalException.class)
    protected ResponseEntity<ExceptionResponse> handleGlobalException(GlobalException e, HttpServletRequest request) {
        GlobalErrorCode code = e.getErrorCode();

        try (MDC.MDCCloseable path = MDC.putCloseable("path", request.getRequestURI());
             MDC.MDCCloseable method = MDC.putCloseable("method", request.getMethod());

             MDC.MDCCloseable httpStatus = MDC.putCloseable("httpStatus", String.valueOf(code.httpStatus().value()));
             MDC.MDCCloseable customMessage = MDC.putCloseable("customMessage", code.message())) {

            logBasedOnLevel(code, e);
        }

        return ResponseEntity.status(code.httpStatus())
                .body(new ExceptionResponse(code.httpStatus().value(), code.message()));
    }

    private void logBasedOnLevel(GlobalErrorCode code, Exception e) {
        String logMessage = String.format("Exception: %s", code.message());
        Level logLevel = code.logEvent();

        if (logLevel == Level.ERROR) {
            log.error(logMessage, e);
        } else if (logLevel == Level.WARN) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
    }
}
