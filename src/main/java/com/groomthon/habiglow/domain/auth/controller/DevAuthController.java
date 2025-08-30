package com.groomthon.habiglow.domain.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.auth.dto.request.MockLoginRequest;
import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.domain.auth.service.DevAuthService;
import com.groomthon.habiglow.global.response.AutoApiResponse;
<<<<<<< HEAD
import com.groomthon.habiglow.global.response.ApiSuccessCode;
=======
import com.groomthon.habiglow.global.response.MemberSuccessCode;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
import com.groomthon.habiglow.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dev/auth")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "local"}) // 개발 환경에서만 활성화
@Tag(name = "개발용 인증 API", description = "개발 환경에서만 사용 가능한 Mock 인증 API")
public class DevAuthController {

	private final DevAuthService devAuthService;

	@PostMapping("/register")
	@Operation(summary = "개발용 Mock 회원가입", description = "테스트용 사용자를 생성합니다.")
	@AutoApiResponse
<<<<<<< HEAD
	@SuccessCode(ApiSuccessCode.MEMBER_CREATED)
=======
	@SuccessCode(MemberSuccessCode.MEMBER_CREATED)
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
	public ResponseEntity<Void> mockRegister(@Valid @RequestBody MockLoginRequest request) {
		log.info("개발용 Mock 회원가입 요청: email={}, socialType={}",
			request.getEmail(), request.getSocialType());

		devAuthService.mockRegister(request);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/mock-login")
	@Operation(summary = "개발용 Mock 로그인", description = "기존 테스트용 사용자로 로그인하여 JWT 토큰을 발급받습니다.")
	@AutoApiResponse
<<<<<<< HEAD
	@SuccessCode(ApiSuccessCode.SOCIAL_LOGIN_SUCCESS)
=======
	@SuccessCode(MemberSuccessCode.SOCIAL_LOGIN_SUCCESS)
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
	public ResponseEntity<TokenResponse> mockLogin(
		@Valid @RequestBody MockLoginRequest request,
		HttpServletResponse response) {

		log.info("개발용 Mock 로그인 요청: email={}, socialType={}",
			request.getEmail(), request.getSocialType());

		TokenResponse tokenResponse = devAuthService.mockLogin(request, response);

		return ResponseEntity.ok(tokenResponse);
	}
}