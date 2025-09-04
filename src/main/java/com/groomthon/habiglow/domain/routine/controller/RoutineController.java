package com.groomthon.habiglow.domain.routine.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.dto.request.CreateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.request.UpdateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineCategoryResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineListResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.AdaptationAction;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.AdaptiveRoutineCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.RoutineAdaptationResultResponse;
import com.groomthon.habiglow.domain.routine.facade.RoutineManagementFacade;
import com.groomthon.habiglow.domain.routine.facade.RoutineQueryFacade;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "루틴 관리 API", description = "사용자 루틴 CRUD, 성장 모드, 카테고리 관리 통합 API")
public class RoutineController {
    
    private final RoutineManagementFacade routineManagementFacade; // Command (CUD)
    private final RoutineQueryFacade routineQueryFacade; // Query (R)
    private final RoutineGrowthService routineGrowthService; // Growth-specific operations
    private final JwtMemberExtractor jwtMemberExtractor;

    @Operation(
        summary = "루틴 생성",
        description = "새로운 루틴을 생성합니다."
    )
    @ApiResponse(responseCode = "200", description = "생성 성공")
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.ROUTINE_CREATED)
    public RoutineResponse createRoutine(
            HttpServletRequest request,
            @Valid @RequestBody CreateRoutineRequest createRequest) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineManagementFacade.createRoutineWithFullValidation(userId, createRequest);
    }
    
    @Operation(
        summary = "내 루틴 목록 조회",
        description = "현재 로그인한 사용자의 모든 루틴을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.ROUTINE_LIST_VIEW)
    public RoutineListResponse getMyRoutines(HttpServletRequest request) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineQueryFacade.getMyRoutines(userId);
    }
    
    @Operation(
        summary = "카테고리별 내 루틴 목록 조회",
        description = "현재 로그인한 사용자의 특정 카테고리 루틴을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @GetMapping("/category")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.ROUTINE_LIST_VIEW)
    public RoutineListResponse getMyRoutinesByCategory(
            HttpServletRequest request,
            @Parameter(description = "루틴 카테고리") @RequestParam RoutineCategory category) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineQueryFacade.getMyRoutinesByCategory(userId, category);
    }
    
    @Operation(
        summary = "루틴 상세 조회",
        description = "특정 루틴의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
    })
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @GetMapping("/{routineId}")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.ROUTINE_VIEW)
    public RoutineResponse getRoutineById(
            HttpServletRequest request,
            @PathVariable Long routineId) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineQueryFacade.getRoutineById(userId, routineId);
    }
    
    @Operation(
        summary = "루틴 수정",
        description = "기존 루틴을 수정합니다. (제목 제외)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
    })
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @PutMapping("/{routineId}")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.ROUTINE_UPDATED)
    public RoutineResponse updateRoutine(
            HttpServletRequest request,
            @PathVariable Long routineId,
            @Valid @RequestBody UpdateRoutineRequest updateRequest) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineManagementFacade.updateRoutineWithValidation(userId, routineId, updateRequest);
    }
    
    @Operation(
        summary = "루틴 삭제",
        description = "특정 루틴을 삭제합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
    })
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @DeleteMapping("/{routineId}")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.ROUTINE_DELETED)
    public void deleteRoutine(
            HttpServletRequest request,
            @PathVariable Long routineId) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        routineManagementFacade.deleteRoutine(userId, routineId);
    }
    
    // ==================== 카테고리 관련 API ====================
    
    @Operation(
        summary = "루틴 카테고리 목록 조회",
        description = "회원이 선택할 수 있는 모든 루틴 카테고리 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/categories")
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public List<RoutineCategoryResponse> getAllCategories() {
        return RoutineCategoryResponse.fromAll();
    }
    
    // ==================== 성장 모드 관련 API ====================
    
    @Operation(
        summary = "적응형 루틴 조정 대상 조회",
        description = "성장(증가) 대상과 감소 대상 루틴을 통합 조회합니다. 전날 기준으로 성장 주기 완료 루틴은 증가 대상, 성장 주기 동안 FULL_SUCCESS가 없는 루틴은 감소 대상입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @GetMapping("/adaptation-check")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public AdaptiveRoutineCheckResponse checkAdaptiveRoutines(HttpServletRequest request) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineGrowthService.checkAdaptiveRoutines(userId);
    }
    
    @Operation(
        summary = "루틴 목표 조정",
        description = "루틴의 목표를 증가, 감소 또는 주기를 리셋합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조정 성공"),
        @ApiResponse(responseCode = "400", description = "조정 조건 미충족", content = @Content),
        @ApiResponse(responseCode = "404", description = "루틴이 존재하지 않음", content = @Content)
    })
    @CustomExceptionDescription(SwaggerResponseDescription.ROUTINE_ERROR)
    @PatchMapping("/{routineId}/target")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public RoutineAdaptationResultResponse adjustRoutineTarget(
        HttpServletRequest request,
        @PathVariable Long routineId,
        @Parameter(description = "조정 액션 (INCREASE, DECREASE, RESET)", example = "INCREASE") 
        @RequestParam AdaptationAction action) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineManagementFacade.executeRoutineAdaptation(userId, routineId, action);
    }
}