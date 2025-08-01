package inu.codin.codin.global.auth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import inu.codin.codin.global.common.response.ExceptionResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        SecurityErrorCode errorCode = SecurityErrorCode.ACCESS_DENIED;

        response.setStatus(errorCode.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ExceptionResponse exceptionResponse = new ExceptionResponse(
                errorCode.httpStatus().value(),
                errorCode.message()
        );

        String jsonResponse = objectMapper.writeValueAsString(exceptionResponse);
        response.getWriter().write(jsonResponse);
    }
}
