package com.groomthon.habiglow.global.oauth2.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.groomthon.habiglow.domain.auth.service.RefreshTokenService;
import com.groomthon.habiglow.global.jwt.JWTUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenService {

	private final JWTUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;


	public String createUserAccessToken(String memberId, String email, String socialUniqueId) {
		return jwtUtil.createAccessToken(memberId, email, socialUniqueId);
	}

	public void sendAccessTokenOnly(HttpServletResponse response, String accessToken) {
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		log.info("OAuth2 AccessToken 전송 완료");
	}

	public String createRefreshTokenForUser(String memberId, String email, String socialUniqueId) {
		return jwtUtil.createRefreshToken(memberId, email, socialUniqueId);
	}


	public void sendAccessAndRefreshToken(HttpServletResponse response, String accessToken, String refreshToken) {
		setAccessToken(response, accessToken);
		setRefreshCookie(response, refreshToken);
		log.info("OAuth2 AccessToken과 RefreshToken 전송 완료");
	}

	public void updateRefreshToken(String memberId, String refreshToken) {
		refreshTokenService.saveToken(memberId, refreshToken);
		log.info("OAuth2 RefreshToken 갱신 완료 - MemberId: {}", memberId);
	}

	private void setAccessToken(HttpServletResponse response, String accessToken) {
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
	}

	private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = jwtUtil.createRefreshTokenCookie(refreshToken);
		response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}