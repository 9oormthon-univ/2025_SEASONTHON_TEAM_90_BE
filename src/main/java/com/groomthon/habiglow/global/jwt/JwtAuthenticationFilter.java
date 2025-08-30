package com.groomthon.habiglow.global.jwt;

import java.io.IOException;

<<<<<<< HEAD
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.groomthon.habiglow.domain.auth.service.BlacklistService;
=======
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import com.groomthon.habiglow.domain.auth.service.BlacklistService;
import com.groomthon.habiglow.domain.member.security.CustomUserDetailsService;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd

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
<<<<<<< HEAD
=======
	private final CustomUserDetailsService userDetailsService;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		jwtUtil.extractAccessToken(request)
			.filter(token -> validateAccessToken(token, response))
<<<<<<< HEAD
			.ifPresent(token -> {
				try {
					String email = jwtUtil.getEmail(token).orElse("");
					String userId = jwtUtil.getId(token).orElse("");

					// JWT에서 추출한 정보로 직접 Authentication 생성
					Authentication authentication = new UsernamePasswordAuthenticationToken(
						userId, // principal: 사용자 ID
						null,   // credentials: JWT 토큰 기반이므로 null
						Collections.singletonList(new SimpleGrantedAuthority("SOCIAL_USER")) // authorities
					);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.info("JWT 기반 SecurityContext 저장 완료: email={}, userId={}", email, userId);
				} catch (Exception e) {
					log.warn("JWT 인증 처리 실패: {}", e.getMessage());
					sendUnauthorized(response, "JWT 토큰 처리 실패");
=======
			.flatMap(jwtUtil::getEmail)
			.ifPresent(email -> {
				try {
					UserDetails userDetails = userDetailsService.loadUserByUsername(email);
					Authentication authentication = new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
					);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.info("SecurityContext에 인증 객체 저장 완료: {}", email);
				} catch (Exception e) {
					log.warn("UserDetails 로딩 실패: {}", e.getMessage());
					sendUnauthorized(response, "회원 인증 실패");
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
				}
			});

		filterChain.doFilter(request, response);
	}

	private boolean validateAccessToken(String token, HttpServletResponse response) {
		if (!jwtUtil.validateToken(token, "access")) {
			log.warn("토큰 검증 실패");
			sendUnauthorized(response, "유효하지 않은 토큰입니다.");
			return false;
		}

		if (blacklistService.isBlacklisted(token)) {
			log.warn("블랙리스트에 등록된 토큰입니다.");
			sendUnauthorized(response, "TOKEN_BLACKLISTED");
			return false;
		}

		return true;
	}

	private void sendUnauthorized(HttpServletResponse response, String message) {
		try {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
		} catch (IOException e) {
			log.error("응답 중 에러 발생", e);
		}
	}
}