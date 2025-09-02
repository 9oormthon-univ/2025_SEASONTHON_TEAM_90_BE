package com.groomthon.habiglow.global.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

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
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
	
	private final SecurityResponseUtils responseUtils;
	
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
	                  AccessDeniedException accessDeniedException) throws IOException, ServletException {
		
		log.warn("Access denied for request: {} {}, reason: {}", 
			request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
		
		responseUtils.sendErrorResponse(response, ErrorCode.ACCESS_DENIED);
	}
}