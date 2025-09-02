package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "핵심 지표")
public class MetricsInfo {
    private int totalRoutines;      // 설정된 루틴 개수
    private Rate overall;           // 주간 전체 실행률
    private List<CategoryRate> categories; // 카테고리별 실행률
}