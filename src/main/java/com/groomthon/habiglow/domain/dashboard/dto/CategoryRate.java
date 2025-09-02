package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "카테고리별 실행률")
public class CategoryRate {
    private String code;   // enum name
    private String label;  // 한글 라벨 등
    private int done;
    private int total;
    private double rate;
}
