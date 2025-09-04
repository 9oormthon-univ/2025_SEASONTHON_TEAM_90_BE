package com.groomthon.habiglow.domain.member.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.member.dto.request.UpdateMemberInterestsRequest;
import com.groomthon.habiglow.domain.member.dto.request.UpdateMemberRequest;
import com.groomthon.habiglow.domain.member.dto.response.MemberInterestsResponse;
import com.groomthon.habiglow.domain.member.dto.response.MemberResponse;
import com.groomthon.habiglow.domain.member.service.MemberInterestService;
import com.groomthon.habiglow.domain.member.service.MemberService;
import com.groomthon.habiglow.global.jwt.JwtMemberExtractor;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "회원 관리 API", description = "로그인한 회원의 정보 조회, 삭제 관련 API")
public class MemberApiController {

	private final MemberService memberService;
	private final MemberInterestService memberInterestService;
	private final JwtMemberExtractor jwtMemberExtractor;

	// 내 정보 조회
	@Operation(
		summary = "내 정보 조회",
		description = "로그인한 회원의 정보를 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 회원"),
		@ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없음")
	})
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.MEMBER_VIEW)
	public MemberResponse getMyInfo(HttpServletRequest request) {
		Long memberId = jwtMemberExtractor.extractMemberId(request);
		return memberService.findById(memberId);
	}

	// 내 계정 삭제
	@Operation(
		summary = "내 계정 삭제",
		description = "로그인한 회원의 계정을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 회원"),
		@ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없음")
	})
	@DeleteMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.MEMBER_DELETED)
	public void deleteMyAccount(HttpServletRequest request) {
		Long memberId = jwtMemberExtractor.extractMemberId(request);
		memberService.deleteById(memberId);
	}

	// 내 관심사 조회
	@Operation(
		summary = "내 관심사 조회",
		description = "로그인한 회원의 루틴 관심사 목록을 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 회원"),
		@ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없음")
	})
	@GetMapping("/me/interests")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public MemberInterestsResponse getMyInterests(HttpServletRequest request) {
		Long memberId = jwtMemberExtractor.extractMemberId(request);
		return memberInterestService.getMemberInterests(memberId);
	}

	// 내 정보 부분 수정
	@Operation(
		summary = "내 정보 부분 수정",
		description = "로그인한 회원의 정보를 부분적으로 수정합니다. 이름, 프로필 이미지 URL, 관심사를 선택적으로 수정할 수 있습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 회원"),
		@ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없음")
	})
	@PatchMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public void updateMyInfo(
		HttpServletRequest request,
		@Valid @RequestBody UpdateMemberRequest requestDto
	) {
		Long memberId = jwtMemberExtractor.extractMemberId(request);
		memberService.updateMemberInfo(memberId, requestDto);
	}

	// 내 관심사 수정
	@Operation(
		summary = "내 관심사 수정",
		description = "로그인한 회원의 루틴 관심사를 수정합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 회원"),
		@ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없음")
	})
	@PutMapping("/me/interests")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public void updateMyInterests(
		HttpServletRequest request,
		@Valid @RequestBody UpdateMemberInterestsRequest requestDto
	) {
		Long memberId = jwtMemberExtractor.extractMemberId(request);
		memberInterestService.updateInterests(memberId, requestDto.getInterests());
	}
}

