package com.groomthon.habiglow.domain.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 분석을 위한 주간 데이터 구조체
 * 기획서의 JSON 포맷을 그대로 따라 구현
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 분석을 위한 주간 데이터")
public class WeeklyAnalysisData {

    @JsonProperty("week_start")
    @Schema(description = "주간 시작일 (월요일)", example = "2025-08-25")
    private String weekStart;

    @JsonProperty("week_end")
    @Schema(description = "주간 종료일 (일요일)", example = "2025-08-31")
    private String weekEnd;

    @Schema(description = "7일간의 일별 데이터")
    private List<DayData> days;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "하루 데이터")
    public static class DayData {

        @Schema(description = "날짜", example = "2025-08-25")
        private String date;

        @Schema(description = "감정 이모지", example = "🙂")
        private String emotion;

        @Schema(description = "루틴 수행 결과 목록")
        private List<RoutineResult> routines;

        @Schema(description = "하루 회고 메모", example = "회의가 길어 영어 학습을 못 함")
        private String note;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "루틴 수행 결과")
    public static class RoutineResult {

        @Schema(description = "루틴 이름", example = "물 마시기")
        private String name;

        @Schema(description = "수행 결과", example = "SUCCESS", allowableValues = {"SUCCESS", "PARTIAL", "FAIL"})
        private String result;
    }
}