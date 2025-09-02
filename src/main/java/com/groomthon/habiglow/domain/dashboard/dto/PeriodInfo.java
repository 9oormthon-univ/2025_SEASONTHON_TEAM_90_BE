package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "기간/완료/네비 정보")
public class PeriodInfo {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String label;          // 예: "9월 셋째 주"
    private boolean isCurrentWeek;
    private boolean isComplete;    // 월~일 모두 지난 주인가
    private NavInfo nav;
}
