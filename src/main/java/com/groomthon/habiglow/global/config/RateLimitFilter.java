package com.groomthon.habiglow.global.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.util.concurrent.RateLimiter;
import com.groomthon.habiglow.global.response.ErrorCode;
import com.groomthon.habiglow.global.util.SecurityResponseUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API 요청에 대한 Rate Limiting을 적용하는 필터
 * IP별로 요청 제한을 설정하여 DDoS 및 무차별 대입 공격을 방지
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap;
    private final SecurityResponseUtils responseUtils;

    // API별 요청 제한 설정 (초당 요청 수)
    private static final double SOCIAL_LOGIN_RATE = 0.1;  // 10초당 1회
    private static final double TOKEN_REFRESH_RATE = 0.17; // 6초당 1회
    private static final double DEFAULT_RATE = 1.0;       // 1초당 1회

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        
        // Rate Limiting이 필요한 API만 처리
        if (isRateLimitRequired(requestUri)) {
            double rate = getRateForUri(requestUri);
            String key = clientIp + ":" + requestUri;
            
            RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(key, 
                k -> RateLimiter.create(rate));
            
            if (!rateLimiter.tryAcquire()) {
                log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
                sendRateLimitError(response);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitRequired(String uri) {
        return uri.equals("/api/auth/social/login") ||
               uri.equals("/api/auth/token/refresh") ||
               uri.equals("/api/auth/token/refresh/full");
    }

    private double getRateForUri(String uri) {
        return switch (uri) {
            case "/api/auth/social/login" -> SOCIAL_LOGIN_RATE;
            case "/api/auth/token/refresh", "/api/auth/token/refresh/full" -> TOKEN_REFRESH_RATE;
            default -> DEFAULT_RATE;
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        responseUtils.sendErrorResponse(response, ErrorCode.TOO_MANY_REQUESTS);
    }
}