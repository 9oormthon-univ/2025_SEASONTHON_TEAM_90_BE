package com.groomthon.habiglow.domain.dashboard.controller;

import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.domain.dashboard.service.WeeklyInsightService;
import com.groomthon.habiglow.global.jwt.JwtMemberExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(
        name = "대시보드 인사이트 API",
        description = "주간 AI 인사이트, 통계를 관리하는 api"
)
@SecurityRequirement(name = "bearerAuth") // Swagger UI에 자물쇠/인증 표시
public class DashboardController {

    private final WeeklyInsightService weeklyInsightService;
    private final JwtMemberExtractor jwtMemberExtractor;

    @Operation(summary = "지난주 AI 분석 생성")
    @ApiResponse(responseCode = "200", description = "성공")
    @PostMapping("/insight/weekly/last")
    @PreAuthorize("isAuthenticated()")
    public WeeklyInsightResponse generateLastWeekInsight(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.generateLastWeekInsight(memberId);
    }

    @Operation(
            summary = "특정 주차 AI 분석 생성",
            description = "weekStart는 해당 주차의 월요일(yyyy-MM-dd). 지난주가 아니고 실데이터가 없으면 204(No Content)."
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "204", description = "해당 주차에 실데이터가 없어 분석을 생성하지 않음")
    @PostMapping("/insight/weekly/specific")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WeeklyInsightResponse> generateSpecificWeekInsight(
            HttpServletRequest request,
            @RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);

        LocalDate lastWeekMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1);

        // 지난주가 아니고, 실데이터가 없으면 204
        if (!weekStart.equals(lastWeekMonday)
                && !weeklyInsightService.hasRealWeekData(memberId, weekStart)) {
            return ResponseEntity.noContent().build();
        }

        WeeklyInsightResponse body = weeklyInsightService.generateSpecificWeekInsight(memberId, weekStart);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "이번주 진행상황 AI 분석")
    @ApiResponse(responseCode = "200", description = "성공")
    @PostMapping("/insight/weekly/current")
    @PreAuthorize("isAuthenticated()")
    public WeeklyInsightResponse generateThisWeekInsight(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.generateThisWeekInsight(memberId);
    }

    @Operation(summary = "분석 가능한 주차 목록 조회")
    @ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/insight/available-weeks")
    @PreAuthorize("isAuthenticated()")
    public List<String> getAvailableWeeks(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.getAvailableWeeks(memberId);
    }

    @Operation(summary = "지난주 완료 여부 확인")
    @ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/insight/last-week-completed")
    @PreAuthorize("isAuthenticated()")
    public boolean isLastWeekCompleted(HttpServletRequest request) {
        Long memberId = jwtMemberExtractor.extractMemberId(request);
        return weeklyInsightService.isLastWeekCompleted(memberId);
    }
}
