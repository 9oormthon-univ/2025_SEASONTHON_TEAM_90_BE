package com.groomthon.habiglow.global.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.groomthon.habiglow.global.exception.jwt.JwtAuthenticationException;
import com.groomthon.habiglow.global.response.ErrorCode;
import com.groomthon.habiglow.global.util.SecurityResponseUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	
	private final SecurityResponseUtils responseUtils;
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, 
	                    AuthenticationException authException) throws IOException, ServletException {
		
		log.warn("Authentication failed: {}", authException.getMessage());
		
		ErrorCode errorCode = determineErrorCode(authException);
		responseUtils.sendErrorResponse(response, errorCode);
	}
	
	private ErrorCode determineErrorCode(AuthenticationException authException) {
		// JWT 관련 예외인 경우 구체적인 에러 코드 반환
		if (authException instanceof JwtAuthenticationException jwtException) {
			log.debug("JWT authentication failed with specific error: {}", jwtException.getErrorCode().getCode());
			return jwtException.getErrorCode();
		}
		
		// 일반적인 인증 실패
		log.debug("General authentication failed, returning UNAUTHORIZED");
		return ErrorCode.UNAUTHORIZED;
	}
}