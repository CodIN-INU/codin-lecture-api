package inu.codin.codin.global.common.exception;

import inu.codin.codin.global.common.response.ExceptionResponse;
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
        StackTraceElement[] stack = e.getStackTrace();

        if (stack != null && stack.length > 0) {
            StackTraceElement stackTraceElement = stack[0];

            log.error("[Exception] 발생 위치: {}.{}({}:{}) | 원인: {} - {}",
                    stackTraceElement.getClassName(),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getFileName(),
                    stackTraceElement.getLineNumber(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
        } else {
            log.error("[Exception] 원인: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
    }

    @ExceptionHandler(GlobalException.class)
    protected ResponseEntity<ExceptionResponse> handleGlobalException(GlobalException e) {
        GlobalErrorCode code = e.getErrorCode();

        try (MDC.MDCCloseable httpStatus = MDC.putCloseable("httpStatus", String.valueOf(code.httpStatus().value()));
             MDC.MDCCloseable customMessage = MDC.putCloseable("customMessage", code.message())) {

            StackTraceElement[] stackTrace = e.getStackTrace();

            // 2. 예외 발생 위치 정보가 있을 경우, 중첩된 try-with-resources로 추가 정보를 관리합니다.
            if (stackTrace.length > 0) {
                StackTraceElement topElement = stackTrace[0];
                try (MDC.MDCCloseable className = MDC.putCloseable("className", topElement.getClassName());
                     MDC.MDCCloseable methodName = MDC.putCloseable("methodName", topElement.getMethodName());
                     MDC.MDCCloseable lineNumber = MDC.putCloseable("lineNumber", String.valueOf(topElement.getLineNumber()))) {

                    // 모든 MDC 정보가 포함된 상태에서 로그를 기록합니다.
                    logBasedOnLevel(code, e);
                }
            } else {
                // 기본 MDC 정보만 포함된 상태에서 로그를 기록합니다.
                logBasedOnLevel(code, e);
            }

        } // 이 블록을 벗어나는 순간, 자동으로 모든 MDC.putCloseable 리소스가 close (remove) 됩니다.

        // 5. 클라이언트에게 보낼 응답 생성
        return ResponseEntity.status(code.httpStatus())
                .body(new ExceptionResponse(code.httpStatus().value(), code.message()));
    }

    private void logBasedOnLevel(GlobalErrorCode code, Exception e) {
        String logMessage = "Custom Exception Occurred";
        Level logLevel = code.logEvent(); // User Gist에서는 logEvent()로 되어있어 getLogLevel()로 수정

        if (logLevel == Level.ERROR) {
            log.error(logMessage, e); // ERROR 레벨일 경우 예외 스택 트레이스 전체를 기록
        } else if (logLevel == Level.WARN) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
    }
}
