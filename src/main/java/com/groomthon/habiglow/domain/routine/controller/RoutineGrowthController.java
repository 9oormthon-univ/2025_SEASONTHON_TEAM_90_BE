package com.groomthon.habiglow.domain.routine.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.routine.dto.response.AdaptiveRoutineCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.DecreaseTargetResponse;
import com.groomthon.habiglow.domain.routine.dto.response.IncreaseTargetResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ResetGrowthCycleResponse;
import com.groomthon.habiglow.domain.routine.service.RoutineGrowthService;
import com.groomthon.habiglow.global.jwt.JwtMemberExtractor;
import com.groomthon.habiglow.global.response.ApiSuccessCode;
import com.groomthon.habiglow.global.response.AutoApiResponse;
import com.groomthon.habiglow.global.response.SuccessCode;
import com.groomthon.habiglow.global.swagger.CustomExceptionDescription;
import com.groomthon.habiglow.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/routines/growth")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "루틴 성장 관리 API", description = "루틴 난이도 자동 조정 및 성장 관련 API")
public class RoutineGrowthController {

	private final RoutineGrowthService routineGrowthService;
	private final JwtMemberExtractor jwtMemberExtractor;

	@Operation(
		summary = "적응형 루틴 조정 대상 조회",
		description = "성장(증가) 대상과 감소 대상 루틴을 통합 조회합니다. 전날 기준으로 성장 주기 완료 루틴은 증가 대상, 성장 주기 동안 FULL_SUCCESS가 없는 루틴은 감소 대상입니다."
	)
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
	@GetMapping("/adaptive-check")
	@PreAuthorize("isAuthenticated()")
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public AdaptiveRoutineCheckResponse checkAdaptiveRoutines(HttpServletRequest request) {
		Long userId = jwtMemberExtractor.extractMemberId(request);
		return routineGrowthService.checkAdaptiveRoutines(userId);
	}

	@Operation(
		summary = "루틴 목표치 증가",
		description = "성장 주기가 완료된 루틴의 목표치를 증가시킵니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "증가 성공"),
		@ApiResponse(responseCode = "400", description = "성장 조건 미충족", content = @Content),
		@ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
	})
	@CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
	@PatchMapping("/{routineId}/increase-target")
	@PreAuthorize("isAuthenticated()")
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public IncreaseTargetResponse increaseTarget(
		HttpServletRequest request,
		@Parameter(description = "루틴 ID", example = "1") @PathVariable Long routineId) {
		Long userId = jwtMemberExtractor.extractMemberId(request);
		return routineGrowthService.increaseRoutineTarget(routineId, userId);
	}

	@Operation(
		summary = "성장 주기 리셋",
		description = "성장 주기가 완료된 루틴의 주기를 리셋합니다. 성장을 거부할 때 사용합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "리셋 성공"),
		@ApiResponse(responseCode = "400", description = "리셋 조건 미충족", content = @Content),
		@ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
	})
	@CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
	@PatchMapping("/{routineId}/reset-cycle")
	@PreAuthorize("isAuthenticated()")
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public ResetGrowthCycleResponse resetGrowthCycle(
		HttpServletRequest request,
		@Parameter(description = "루틴 ID", example = "1") @PathVariable Long routineId) {
		Long userId = jwtMemberExtractor.extractMemberId(request);
		return routineGrowthService.resetGrowthCycle(routineId, userId);
	}

	@Operation(
		summary = "루틴 목표치 감소",
		description = "감소 조건이 충족된 루틴의 목표치를 감소시킵니다. 최소값 이하로는 감소하지 않습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "감소 성공"),
		@ApiResponse(responseCode = "400", description = "감소 조건 미충족", content = @Content),
		@ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
	})
	@CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
	@PatchMapping("/{routineId}/decrease-target")
	@PreAuthorize("isAuthenticated()")
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public DecreaseTargetResponse decreaseTarget(
		HttpServletRequest request,
		@Parameter(description = "루틴 ID", example = "1") @PathVariable Long routineId) {
		Long userId = jwtMemberExtractor.extractMemberId(request);
		return routineGrowthService.decreaseRoutineTarget(routineId, userId);
	}
}