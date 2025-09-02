package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Schema(name = "WeeklyDashboardDto", description = "주간 대시보드 통계 응답")
public class WeeklyDashboardDto {

    @Schema(description = "기간 정보")
    private PeriodInfo period;

    @Schema(description = "핵심 지표")
    private MetricsInfo metrics;

    @Schema(description = "감정 분포 (HAPPY, SOSO, SAD, MAD)")
    private Map<String, Integer> emotionDistribution;

    @Schema(description = "일별 완성률 및 감정 (현재 요일까지만)")
    private List<DailyCompletionInfo> dailyCompletion;

    @Schema(description = "루틴별 성과")
    private List<RoutinePerformanceInfo> routinePerformance;
}