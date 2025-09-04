package com.groomthon.habiglow.domain.routine.dto.response.adaptation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "적응 타입")
public enum AdaptationType {
    
    @Schema(description = "성장 (목표 증가)")
    GROWTH,
    
    @Schema(description = "감소 (목표 감소)")
    REDUCTION,
    
    @Schema(description = "혼합 (성장 + 감소)")
    MIXED
}