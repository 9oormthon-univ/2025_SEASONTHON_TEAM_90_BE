package com.groomthon.habiglow.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.auth.entity.RefreshToken;
import com.groomthon.habiglow.domain.auth.repository.RefreshTokenRepository;
import com.groomthon.habiglow.global.jwt.JWTUtil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 검증을 담당하는 컴포넌트
 * 재사용 가능한 토큰 검증 로직을 중앙화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidator {

	private final JWTUtil jwtUtil;
	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * Refresh Token의 종합적 검증
	 */
	public TokenValidationResult validateRefreshToken(String refreshToken) {
		if (!jwtUtil.isRefreshToken(refreshToken)) {
			log.warn("Invalid refresh token format or expired");
			return TokenValidationResult.invalid("Invalid or expired refresh token");
		}

		Optional<String> memberIdOpt = jwtUtil.getId(refreshToken);
		if (memberIdOpt.isEmpty()) {
			log.warn("Cannot extract member ID from refresh token");
			return TokenValidationResult.invalid("Invalid token payload");
		}

		String memberId = memberIdOpt.get();

		RefreshToken storedToken = refreshTokenRepository.findById(memberId).orElse(null);
		if (storedToken == null || !constantTimeEquals(storedToken.getToken(), refreshToken)) {
			log.warn("Refresh token not found or mismatched for member: {}", memberId);
			return TokenValidationResult.invalid("Token not found or mismatched");
		}

		Optional<String> emailOpt = jwtUtil.getEmail(refreshToken);
		Optional<String> socialUniqueIdOpt = jwtUtil.getSocialUniqueId(refreshToken);

		if (emailOpt.isEmpty()) {
			log.warn("Cannot extract email from refresh token");
			return TokenValidationResult.invalid("Invalid token claims");
		}

		log.debug("Refresh token validation successful for member: {}", memberId);
		return TokenValidationResult.valid(memberId, emailOpt.get(), socialUniqueIdOpt.orElse(null));
	}

	/**
	 * Access Token의 종합적 검증 (형식, 만료, 블랙리스트)
	 */
	public TokenValidationResult validateAccessToken(String accessToken) {
		if (!jwtUtil.isAccessToken(accessToken)) {
			log.warn("Invalid access token format or expired");
			return TokenValidationResult.invalid("Invalid or expired access token");
		}

		Optional<String> memberIdOpt = jwtUtil.getId(accessToken);
		Optional<String> emailOpt = jwtUtil.getEmail(accessToken);
		
		if (memberIdOpt.isEmpty() || emailOpt.isEmpty()) {
			log.warn("Cannot extract claims from access token");
			return TokenValidationResult.invalid("Invalid token payload");
		}

		log.debug("Access token validation successful for member: {}", memberIdOpt.get());
		return TokenValidationResult.valid(memberIdOpt.get(), emailOpt.get(), 
			jwtUtil.getSocialUniqueId(accessToken).orElse(null));
	}

	/**
	 * Access Token의 블랙리스트 및 만료 검증 (기존 호환성)
	 */
	public boolean isValidAccessToken(String accessToken) {
		return jwtUtil.isAccessToken(accessToken);
	}

	/**
	 * 토큰 검증 결과를 캡슐화하는 클래스
	 */
	@Getter
	public static class TokenValidationResult {
		private final boolean valid;
		private final String memberId;
		private final String email;
		private final String socialUniqueId;
		private final String errorMessage;

		private TokenValidationResult(boolean valid, String memberId, String email, String socialUniqueId, String errorMessage) {
			this.valid = valid;
			this.memberId = memberId;
			this.email = email;
			this.socialUniqueId = socialUniqueId;
			this.errorMessage = errorMessage;
		}

		public static TokenValidationResult valid(String memberId, String email, String socialUniqueId) {
			return new TokenValidationResult(true, memberId, email, socialUniqueId, null);
		}

		public static TokenValidationResult invalid(String errorMessage) {
			return new TokenValidationResult(false, null, null, null, errorMessage);
		}
	}

	/**
	 * Timing Attack을 방지하는 상수 시간 문자열 비교
	 */
	private boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) {
			return a == b;
		}
		
		byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
		byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
		
		return MessageDigest.isEqual(aBytes, bBytes);
	}
}