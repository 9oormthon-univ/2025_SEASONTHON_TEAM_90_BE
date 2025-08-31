package com.groomthon.habiglow.domain.dashboard.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 주간 분석 응답 DTO
 * 기획서의 JSON 스키마를 그대로 구현
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "HabiGlow AI 주간 분석 결과")
public class WeeklyInsightResponse {

    @JsonProperty("week_range")
    @Schema(description = "분석 기간", example = "2025-08-25 ~ 2025-08-31")
    private String weekRange;

    @JsonProperty("mood_daily")
    @Schema(description = "7일간 일별 감정 이모지", example = "[\"🙂\",\"🙂\",\"😐\",\"🙂\",\"😀\",\"🙂\",\"🙂\"]")
    private List<String> moodDaily;

    @JsonProperty("mood_trend")
    @Schema(description = "감정 변화 추세", example = "안정", allowableValues = {"상승", "하락", "안정"})
    private String moodTrend;

    @JsonProperty("weekly_summary")
    @Schema(description = "주간 요약 (80자 이내)", example = "이번 주 기록은 안정적이에요. 수분과 스트레칭의 꾸준함이 돋보여요.")
    private String weeklySummary;

    @JsonProperty("good_points")
    @Schema(description = "잘한 점 목록 (1~3개, 각 60자 이내)",
            example = "[\"물 마시기 6일 지속\", \"스트레칭 5회 유지\"]")
    private List<String> goodPoints;

    @JsonProperty("failure_patterns")
    @Schema(description = "실패 패턴 목록 (0~3개, 각 60자 이내)",
            example = "[\"회의 있는 날 영어 누락 반복\"]")
    private List<String> failurePatterns;

    @Schema(description = "공감 메시지 (80자 이내)",
            example = "바쁜 일정 속에서도 기본 루틴을 지키신 점이 인상적이에요.")
    private String empathy;

    @Schema(description = "응원 메시지 (80자 이내)",
            example = "회의 날엔 영어를 10분 미니 세션으로 가볍게 이어가보세요!")
    private String encouragement;
}