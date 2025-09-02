package com.groomthon.habiglow.domain.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(name = "PeriodInfo", description = "주간 기간 정보")
public class PeriodInfo {
    @Schema(description = "주 시작일(월요일)", example = "2025-08-25")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "주 종료일(일요일)", example = "2025-08-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "기간 라벨", example = "이번 주")
    private String label;
}