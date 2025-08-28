package com.groomthon.habiglow.domain.member.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.member.dto.response.MemberResponse;
import com.groomthon.habiglow.domain.member.service.MemberService;
import com.groomthon.habiglow.global.response.AutoApiResponse;
import com.groomthon.habiglow.global.response.MemberSuccessCode;
import com.groomthon.habiglow.global.response.SuccessCode;
import com.groomthon.habiglow.global.swagger.CustomExceptionDescription;
import com.groomthon.habiglow.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "소셜 회원 관리 API", description = "소셜 로그인 사용자 조회, 삭제 관련 API")
public class UserApiController {

	private final MemberService memberService;

	// 전체 사용자 목록 조회
	@Operation(
		summary = "전체 사용자 목록 조회",
		description = "가입된 모든 사용자의 정보를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(MemberSuccessCode.MEMBER_VIEW)
	public List<MemberResponse> findAll() {
		return memberService.findAll();
	}

	// 특정 사용자 조회
	@Operation(
		summary = "특정 사용자 조회",
		description = "사용자 ID를 기준으로 회원 정보를 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "해당 사용자가 존재하지 않음", content = @Content)
	})
	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(MemberSuccessCode.MEMBER_VIEW)
	public MemberResponse findById(@PathVariable Long id) {
		return memberService.findById(id);
	}


	// 사용자 삭제
	@Operation(
		summary = "사용자 삭제",
		description = "지정한 사용자 ID에 해당하는 회원을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "해당 사용자가 존재하지 않음", content = @Content)
	})
	@DeleteMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	@CustomExceptionDescription(SwaggerResponseDescription.MEMBER_ERROR)
	@SuccessCode(MemberSuccessCode.MEMBER_DELETED)
	public void delete(@PathVariable Long id) {
		memberService.deleteById(id);
	}
}