package com.groomthon.habiglow.domain.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.domain.dashboard.service.WeeklyInsightService;
import com.groomthon.habiglow.global.jwt.JwtMemberExtractor;
import com.groomthon.habiglow.global.response.ApiSuccessCode;
import com.groomthon.habiglow.global.response.AutoApiResponse;
import com.groomthon.habiglow.global.response.SuccessCode;
import com.groomthon.habiglow.global.swagger.CustomExceptionDescription;
import com.groomthon.habiglow.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * HabiGlow AI 대시보드 컨트롤러
 *
 * 주간 습관 분석, 감정 변화 패턴 분석,
 * 성공/실패 패턴에 대한 공감과 응원 메시지 제공
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "AI 대시보드 API", description = "HabiGlow AI 기반 주간 습관 분석 및 인사이트 제공")
public class DashboardController {

    private final WeeklyInsightService weeklyInsightService;
    private final JwtMemberExtractor jwtMemberExtractor;

    @Operation(
            summary = "지난주 AI 분석 생성",
            description = "지난주 월~일 데이터를 기반으로 습관 패턴과 감정 변화를 분석하여 공감과 응원 메시지를 제공합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 분석 성공"),
            @ApiResponse(responseCode = "400", description = "분석할 데이터 부족"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "AI 분석 처리 실패")
    })
    @PostMapping("/insight/weekly/last")
    @PreAuthorize("isAuthenticated()")
    @CustomExceptionDescription(SwaggerResponseDescription.AI_ANALYSIS_ERROR)
    @SuccessCode(ApiSuccessCode.AI_INSIGHT_SUCCESS)
    public WeeklyInsightResponse generateLastWeekInsight(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.generateLastWeekInsight(memberId);
    }

    @Operation(
            summary = "특정 주차 AI 분석 생성",
            description = "지정한 주차(월요일 시작)의 데이터를 기반으로 AI 분석을 수행합니다. 과거 주차만 분석 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 분석 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 주차 또는 분석할 데이터 부족"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/insight/weekly/specific")
    @PreAuthorize("isAuthenticated()")
    @CustomExceptionDescription(SwaggerResponseDescription.AI_ANALYSIS_ERROR)
    @SuccessCode(ApiSuccessCode.AI_INSIGHT_SUCCESS)
    public WeeklyInsightResponse generateWeekInsight(
            HttpServletRequest request,
            @Parameter(description = "분석할 주차의 월요일 날짜", example = "2025-08-25")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart) {

        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.generateWeekInsight(memberId, weekStart);
    }

    @Operation(
            summary = "이번주 진행상황 AI 분석",
            description = "이번주 현재까지의 데이터를 기반으로 진행상황을 분석합니다. (참고용)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 분석 성공"),
            @ApiResponse(responseCode = "400", description = "분석할 데이터 부족"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/insight/weekly/current")
    @PreAuthorize("isAuthenticated()")
    @CustomExceptionDescription(SwaggerResponseDescription.AI_ANALYSIS_ERROR)
    @SuccessCode(ApiSuccessCode.AI_INSIGHT_SUCCESS)
    public WeeklyInsightResponse generateThisWeekInsight(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.generateThisWeekInsight(memberId);
    }

    @Operation(
            summary = "분석 가능한 주차 목록 조회",
            description = "AI 분석이 가능한 최근 4주간의 주차 목록을 반환합니다. (월요일 날짜 기준)"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/insight/available-weeks")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public List<LocalDate> getAvailableWeeks(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.getAvailableWeeks(memberId);
    }

    @Operation(
            summary = "지난주 완료 여부 확인",
            description = "현재 시점에서 지난주가 완료되어 분석 가능한지 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "확인 성공")
    @GetMapping("/insight/last-week-completed")
    @PreAuthorize("isAuthenticated()")
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public boolean isLastWeekCompleted() {
        return weeklyInsightService.isLastWeekCompleted();
    }
}