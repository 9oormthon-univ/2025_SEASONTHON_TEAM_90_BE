package com.groomthon.habiglow.domain.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.domain.auth.service.AuthenticationService;
import com.groomthon.habiglow.global.response.AutoApiResponse;
<<<<<<< HEAD
import com.groomthon.habiglow.global.response.ApiSuccessCode;
=======
import com.groomthon.habiglow.global.response.MemberSuccessCode;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
import com.groomthon.habiglow.global.response.SuccessCode;
import com.groomthon.habiglow.global.swagger.CustomExceptionDescription;
import com.groomthon.habiglow.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "인증 API", description = "소셜 로그인 전용 JWT 토큰 관리")
public class AuthApiController {

	private final AuthenticationService authenticationService;

	@Operation(summary = "Access Token 재발급")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공"),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
	})
	@PostMapping("/token/refresh")
	@CustomExceptionDescription(SwaggerResponseDescription.AUTH_ERROR)
<<<<<<< HEAD
	@SuccessCode(ApiSuccessCode.TOKEN_REISSUE_SUCCESS)
=======
	@SuccessCode(MemberSuccessCode.TOKEN_REISSUE_SUCCESS)
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
	public TokenResponse refreshAccessToken(
		HttpServletRequest request,
		HttpServletResponse response) {

		return authenticationService.refreshAccessToken(request, response);
	}

	@Operation(summary = "Access + Refresh Token 모두 재발급")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공"),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
	})
	@PostMapping("/token/refresh/full")
	@CustomExceptionDescription(SwaggerResponseDescription.AUTH_ERROR)
<<<<<<< HEAD
	@SuccessCode(ApiSuccessCode.TOKEN_REISSUE_FULL_SUCCESS)
=======
	@SuccessCode(MemberSuccessCode.TOKEN_REISSUE_FULL_SUCCESS)
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
	public TokenResponse refreshAllTokens(
		HttpServletRequest request,
		HttpServletResponse response) {

		return authenticationService.refreshAllTokens(request, response);
	}

	@Operation(summary = "로그아웃")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공")
	})
	@PostMapping("/logout")
	@CustomExceptionDescription(SwaggerResponseDescription.AUTH_ERROR)
<<<<<<< HEAD
	@SuccessCode(ApiSuccessCode.LOGOUT_SUCCESS)
=======
	@SuccessCode(MemberSuccessCode.LOGOUT_SUCCESS)
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
	public void logout(
		HttpServletRequest request,
		HttpServletResponse response) {

		authenticationService.logout(request, response);
	}
<<<<<<< HEAD
}
=======
}
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
