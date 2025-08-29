package com.groomthon.habiglow.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.request.SocialLoginRequest;
import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.jwt.JWTUtil;
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

		String accessToken = jwtUtil.createAccessTokenSafe(validation.getMemberId(), validation.getEmail(), validation.getSocialUniqueId());
		response.setHeader("Authorization", "Bearer " + accessToken);

		log.info("Access token refreshed for member: {}", validation.getMemberId());
		return TokenResponse.accessOnly(
			extractAccessTokenFromResponse(response),
			getAccessTokenExpirySeconds()
		);
	}

	@Transactional
	public TokenResponse refreshAllTokens(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromRequest(request);

		TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		refreshTokenService.deleteRefreshToken(validation.getMemberId());
		
		String accessToken = jwtUtil.createAccessTokenSafe(validation.getMemberId(), validation.getEmail(), validation.getSocialUniqueId());
		String newRefreshToken = jwtUtil.createRefreshTokenSafe(validation.getMemberId(), validation.getEmail(), validation.getSocialUniqueId());
		
		refreshTokenService.saveRefreshToken(Long.valueOf(validation.getMemberId()), newRefreshToken);
		
		response.setHeader("Authorization", "Bearer " + accessToken);
		response.setHeader("Set-Cookie", jwtUtil.createRefreshTokenCookie(newRefreshToken).toString());

		log.info("Full token refresh completed for member: {}", validation.getMemberId());
		return TokenResponse.withRefresh(
			extractAccessTokenFromResponse(response),
			getAccessTokenExpirySeconds()
		);
	}

	@Transactional
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		jwtUtil.extractRefreshToken(request)
			.filter(jwtUtil::isRefreshToken)
			.flatMap(jwtUtil::getId)
			.ifPresent(memberId -> {
				refreshTokenService.deleteRefreshToken(memberId);
				log.info("Refresh token deleted for member: {}", memberId);
			});

		response.setHeader("Set-Cookie", jwtUtil.invalidateRefreshToken().toString());

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

	private String extractAccessTokenFromResponse(HttpServletResponse response) {
		String authHeader = response.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		return null;
	}

	private Long getAccessTokenExpirySeconds() {
		return jwtUtil.getAccessTokenExpiration() / 1000;
	}

}
