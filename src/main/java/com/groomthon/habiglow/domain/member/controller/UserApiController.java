package com.groomthon.habiglow.domain.member.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.member.dto.response.MemberResponse;
import com.groomthon.habiglow.domain.member.service.MemberService;
import com.groomthon.habiglow.global.jwt.JwtUserExtractor;
import com.groomthon.habiglow.global.response.ApiSuccessCode;
import com.groomthon.habiglow.global.response.AutoApiResponse;
import com.groomthon.habiglow.global.response.SuccessCode;
import com.groomthon.habiglow.global.swagger.CustomExceptionDescription;
import com.groomthon.habiglow.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "로그인한 사용자의 정보 조회, 삭제 관련 API")
public class UserApiController {

	private final MemberService memberService;
	private final JwtUserExtractor jwtUserExtractor;

	// 내 정보 조회
	@Operation(
		summary = "내 정보 조회",
		description = "로그인한 사용자의 정보를 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
	})
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.MEMBER_VIEW)
	public MemberResponse getMyInfo(HttpServletRequest request) {
		Long userId = jwtUserExtractor.extractUserId(request);
		return memberService.findById(userId);
	}

	// 내 계정 삭제
	@Operation(
		summary = "내 계정 삭제",
		description = "로그인한 사용자의 계정을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
	})
	@DeleteMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.MEMBER_DELETED)
	public void deleteMyAccount(HttpServletRequest request) {
		Long userId = jwtUserExtractor.extractUserId(request);
		memberService.deleteById(userId);
	}
}

