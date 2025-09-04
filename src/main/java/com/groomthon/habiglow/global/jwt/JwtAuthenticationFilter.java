package com.groomthon.habiglow.global.jwt;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.groomthon.habiglow.domain.auth.service.BlacklistService;
import com.groomthon.habiglow.global.exception.jwt.BlacklistedJwtException;
import com.groomthon.habiglow.global.exception.jwt.InvalidJwtSignatureException;
import com.groomthon.habiglow.global.exception.jwt.JwtAuthenticationException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JWTUtil jwtUtil;
	private final BlacklistService blacklistService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		Optional<String> token = jwtUtil.extractAccessToken(request);
		
		if (token.isPresent()) {
			try {
				validateAndSetAuthentication(token.get());
			} catch (JwtAuthenticationException e) {
				SecurityContextHolder.clearContext();
				request.setAttribute("jwtAuthenticationException", e);
			}
		}
		
		filterChain.doFilter(request, response);
	}

	private void validateAndSetAuthentication(String token) {
		// JWT 토큰 검증 - 명시적으로 Access Token 타입 확인
		if (!jwtUtil.isAccessToken(token)) {
			log.warn("유효하지 않은 Access Token입니다");
			throw new InvalidJwtSignatureException();
		}
		
		// 블랙리스트 검증
		if (blacklistService.isBlacklisted(token)) {
			log.warn("블랙리스트에 등록된 토큰입니다.");
			throw new BlacklistedJwtException();
		}
		
		// 인증 설정
		setAuthentication(token);
	}
	
	private void setAuthentication(String token) {
		try {
			String email = jwtUtil.getEmail(token).orElse("");
			String userId = jwtUtil.getId(token).orElse("");

			Authentication authentication = new UsernamePasswordAuthenticationToken(
				userId, // principal: 사용자 ID
				null,   // credentials: JWT 토큰 기반이므로 null
				Collections.singletonList(new SimpleGrantedAuthority("SOCIAL_USER")) // authorities
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			log.debug("JWT 기반 SecurityContext 저장 완료: email={}, userId={}", email, userId);
		} catch (Exception e) {
			log.warn("JWT 인증 처리 실패: {}", e.getMessage());
			throw new InvalidJwtSignatureException(e);
		}
	}
}