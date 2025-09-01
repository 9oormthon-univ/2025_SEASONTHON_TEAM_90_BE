package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 주간 입력 스냅샷(정규화 형태)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "WeeklyAnalysisData", description = "AI 분석 입력을 위한 주간 스냅샷")
public class WeeklyAnalysisData {

    @Schema(description = "사용자 ID", example = "1")
    private Long memberId;

    @Schema(description = "주 시작일(월요일)", format = "date", example = "2025-08-25")
    private LocalDate weekStart;

    @Schema(description = "주 종료일(일요일)", format = "date", example = "2025-08-31")
    private LocalDate weekEnd;

    @Schema(description = "요일별 기록 목록")
    private List<DayStat> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WeeklyAnalysisData.DayStat", description = "개별 일자 통계")
    public static class DayStat {
        @Schema(description = "날짜", format = "date", example = "2025-08-25")
        private LocalDate date;

        @Schema(description = "성공 여부", example = "true")
        private Boolean success;

        @Schema(description = "감정(이모지)", example = "🙂")
        private String mood;

        @Schema(description = "메모/노트", example = "출근 전 20분 루틴 완료")
        private String note;
    }
}
