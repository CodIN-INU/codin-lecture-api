package inu.codin.codin.global.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 토큰 추출 및 파싱 유틸리티
 */
@Slf4j
public class TokenUtils {

    /**
     * Extracts an access token from the given HTTP request.
     *
     * This method first attempts to retrieve a Bearer token from the "Authorization" header.
     * If not found or improperly formatted, it then searches for a cookie named "x-access-token".
     * Returns the extracted token if present and non-empty; otherwise, returns {@code null}.
     *
     * @param request the HTTP request from which to extract the access token
     * @return the extracted access token, or {@code null} if none is found
     */
    public static String extractToken(HttpServletRequest request) {
        String bearerToken = null;
        // 1. Authorization 헤더에서 토큰 추출 (우선순위 1)
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            bearerToken = authHeader.substring(7);
            log.debug("[extractToken] Authorization 헤더에서 토큰 추출: 성공");
        } else {
            log.debug("[extractToken] Authorization 헤더: {}", authHeader != null ? "형식 오류" : "없음");
        }

        // 2. 쿠키에서 토큰 추출 (우선순위 2)
        if (!StringUtils.hasText(bearerToken)) {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("x-access-token".equals(cookie.getName())) {
                        bearerToken = cookie.getValue();
                        break;
                    }
                }
            }
            log.debug("[extractToken] Cookie에서 추출한 토큰: {}", bearerToken != null ? "존재" : "없음");
        }

        return StringUtils.hasText(bearerToken) ? bearerToken : null;
    }
}