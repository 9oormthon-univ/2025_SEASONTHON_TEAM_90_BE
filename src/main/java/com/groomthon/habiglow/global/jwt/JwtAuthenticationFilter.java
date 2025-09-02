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
import com.groomthon.habiglow.global.config.properties.SecurityProperties;

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
	private final SecurityProperties securityProperties;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		
		// Public URL인 경우 JWT 필터 건너뛰기
		for (String publicUrl : securityProperties.getPublicUrlsArray()) {
			if (requestURI.matches(publicUrl.replace("/**", "/.*").replace("*", ".*"))) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		Optional<String> tokenOpt = jwtUtil.extractAccessToken(request);
		
		if (tokenOpt.isPresent()) {
			String token = tokenOpt.get();
			
			// 토큰 검증 실패 시 필터 체인 중단
			if (!validateAccessToken(token, response)) {
				return;
			}
			
			// 인증 정보 설정
			setAuthentication(token);
		}
		
		// 토큰이 없거나 유효한 경우에만 필터 체인 계속 진행
		filterChain.doFilter(request, response);
	}

	private boolean validateAccessToken(String token, HttpServletResponse response) {
		if (!jwtUtil.validateToken(token, "access")) {
			log.warn("토큰 검증 실패");
			return false;
		}

		if (blacklistService.isBlacklisted(token)) {
			log.warn("블랙리스트에 등록된 토큰입니다.");
			return false;
		}

		return true;
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
			log.info("JWT 기반 SecurityContext 저장 완료: email={}, userId={}", email, userId);
		} catch (Exception e) {
			log.warn("JWT 인증 처리 실패: {}", e.getMessage());
			SecurityContextHolder.clearContext();
		}
	}
}