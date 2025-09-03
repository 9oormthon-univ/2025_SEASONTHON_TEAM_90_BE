package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "주차 네비게이션 정보")
public class NavInfo {
    private boolean hasPrev;
    private boolean hasNext;
    private LocalDate prevWeekStart;
    private LocalDate nextWeekStart;
}
