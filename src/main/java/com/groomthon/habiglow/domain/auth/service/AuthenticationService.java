package com.groomthon.habiglow.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.jwt.JwtTokenService;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

	private final JWTUtil jwtUtil;
	private final JwtTokenService jwtTokenService;
	private final TokenValidator tokenValidator;
	private final RefreshTokenService refreshTokenService;
	private final BlacklistService blacklistService;


	/**
	 * 토큰 재발급 (RTR 적용)
	 * Refresh Token을 사용하여 Access Token과 Refresh Token을 모두 재발급합니다.
	 * 보안을 위해 기존 Refresh Token은 무효화되고 새로운 토큰들이 발급됩니다.
	 */
	@Transactional
	public TokenResponse refreshTokens(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		refreshTokenService.deleteRefreshToken(validation.getMemberId());

		TokenResponse tokenResponse = jwtTokenService.reissueAllTokens(response, validation.getMemberId(), validation.getEmail(), validation.getSocialUniqueId());

		log.info("Token refreshed with RTR for member: {}", validation.getMemberId());
		return tokenResponse;
	}

	@Transactional
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		// Refresh Token 삭제
		jwtUtil.extractRefreshToken(request)
			.filter(jwtUtil::isRefreshToken)
			.flatMap(jwtUtil::getId)
			.ifPresent(memberId -> {
				refreshTokenService.deleteRefreshToken(memberId);
				log.info("Refresh token deleted for member: {}", memberId);
			});

		// Refresh Token 쿠키 무효화
		jwtTokenService.expireRefreshCookie(response);

		// Access Token 블랙리스트 추가
		jwtUtil.extractAccessToken(request)
			.filter(jwtUtil::isAccessToken)
			.ifPresent(accessToken -> {
				jwtUtil.getExpiration(accessToken).ifPresent(expiration ->
					blacklistService.addToBlacklist(accessToken, expiration)
				);
			});

		log.info("Logout completed");
	}


	private String extractRefreshTokenFromRequest(HttpServletRequest request) {
		return jwtUtil.extractRefreshToken(request)
			.orElseThrow(() -> new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
	}

}
