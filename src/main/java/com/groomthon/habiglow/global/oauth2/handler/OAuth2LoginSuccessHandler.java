package com.groomthon.habiglow.global.oauth2.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.global.dto.CommonApiResponse;
import com.groomthon.habiglow.global.oauth2.dto.OAuthLoginResponse;
import com.groomthon.habiglow.global.oauth2.service.OAuth2TokenService;
import com.groomthon.habiglow.global.oauth2.user.CustomOAuth2User;
<<<<<<< HEAD
import com.groomthon.habiglow.global.response.ApiSuccessCode;
=======
import com.groomthon.habiglow.global.response.MemberSuccessCode;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuth2TokenService oAuth2TokenService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException, ServletException {
		log.info("OAuth2 Login 성공!");

		try {
			CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

			// Role 시스템 제거로 모든 사용자를 일반 사용자로 처리
			handleNormalLogin(response, oAuth2User);

		} catch (Exception e) {
			log.error("OAuth2 로그인 성공 후 처리 중 에러 발생", e);
			throw e;
		}
	}


	private void handleNormalLogin(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
		log.info("일반 사용자 소셜 로그인 성공: email={}, socialType={}",
			oAuth2User.getEmail(), oAuth2User.getSocialType());

		String accessToken = oAuth2TokenService.createUserAccessToken(
			oAuth2User.getMemberId(),
			oAuth2User.getEmail(),
			oAuth2User.getSocialUniqueId()
		);
		String refreshToken = oAuth2TokenService.createRefreshTokenForUser(
			oAuth2User.getMemberId(),
			oAuth2User.getEmail(),
			oAuth2User.getSocialUniqueId()
		);

		oAuth2TokenService.sendAccessAndRefreshToken(response, accessToken, refreshToken);
		oAuth2TokenService.updateRefreshToken(oAuth2User.getMemberId(), refreshToken);

		// 응답 데이터 생성
		OAuthLoginResponse oAuthLoginResponse = OAuthLoginResponse.builder()
			.accessToken(accessToken)
			.email(oAuth2User.getEmail())
			.name(oAuth2User.getName())
			.socialType(oAuth2User.getSocialType().name())
			.build();

		CommonApiResponse<OAuthLoginResponse> apiResponse = CommonApiResponse.success(
<<<<<<< HEAD
			ApiSuccessCode.SOCIAL_LOGIN_SUCCESS, oAuthLoginResponse);
=======
			MemberSuccessCode.SOCIAL_LOGIN_SUCCESS, oAuthLoginResponse);
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd

		sendJsonResponse(response, apiResponse);

		log.info("소셜 로그인 성공 응답 전송 완료: email={}", oAuth2User.getEmail());
	}

	private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(data));
	}
<<<<<<< HEAD
}
=======
}
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
