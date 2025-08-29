package com.groomthon.habiglow.global.jwt;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.groomthon.habiglow.domain.auth.service.RefreshTokenService;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

	private final JWTUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;

	public void issueTokens(HttpServletResponse response, MemberEntity member) {
		String memberId = member.getId().toString();
		TokenPair tokens = createTokenPair(memberId, member.getMemberEmail(), member.getSocialUniqueId());

		refreshTokenService.saveToken(memberId, tokens.refreshToken);

		setAccessToken(response, tokens.accessToken);
		setRefreshCookie(response, tokens.refreshToken);

		log.info("Access / Refresh 토큰 발급 완료 - Member: {}", member.getMemberEmail());
	}

	public void reissueAccessToken(HttpServletResponse response, String memberId, String email, String socialUniqueId) {
		String accessToken = jwtUtil.createAccessTokenSafe(memberId, email, socialUniqueId);
		setAccessToken(response, accessToken);
		log.info("Access 토큰 재발급 완료 - Member: {}", email);
	}

	public void reissueAllTokens(HttpServletResponse response, String memberId, String email, String socialUniqueId) {
		TokenPair tokens = createTokenPair(memberId, email, socialUniqueId);

		refreshTokenService.saveToken(memberId, tokens.refreshToken);

		setAccessToken(response, tokens.accessToken);
		setRefreshCookie(response, tokens.refreshToken);

		log.info("Access / Refresh 토큰 모두 재발급 완료 - Member: {}", email);
	}


	public void expireRefreshCookie(HttpServletResponse response) {
		ResponseCookie expired = jwtUtil.invalidateRefreshToken();
		response.setHeader(HttpHeaders.SET_COOKIE, expired.toString());
	}

	private void setAccessToken(HttpServletResponse response, String accessToken) {
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
	}

	private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = jwtUtil.createRefreshTokenCookie(refreshToken);
		response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private TokenPair createTokenPair(String memberId, String email, String socialUniqueId) {
		String accessToken = jwtUtil.createAccessTokenSafe(memberId, email, socialUniqueId);
		String refreshToken = jwtUtil.createRefreshTokenSafe(memberId, email, socialUniqueId);
		return new TokenPair(accessToken, refreshToken);
	}


	private static class TokenPair {
		final String accessToken;
		final String refreshToken;

		TokenPair(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}
	}
}