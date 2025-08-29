package com.groomthon.habiglow.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.request.SocialLoginRequest;
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
	private final SocialAuthService socialAuthService;

	public TokenResponse socialLogin(SocialLoginRequest request) {
		return socialAuthService.authenticateWithSocialToken(request);
	}


	@Transactional
	public TokenResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		TokenResponse tokenResponse = jwtTokenService.reissueAccessToken(response, validation.getMemberId(), validation.getEmail(), validation.getSocialUniqueId());

		log.info("Access token refreshed for member: {}", validation.getMemberId());
		return tokenResponse;
	}

	@Transactional
	public TokenResponse refreshAllTokens(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		refreshTokenService.deleteRefreshToken(validation.getMemberId());
		TokenResponse tokenResponse = jwtTokenService.reissueAllTokens(response, validation.getMemberId(), validation.getEmail(), validation.getSocialUniqueId());

		log.info("Full token refresh completed for member: {}", validation.getMemberId());
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
