package com.groomthon.habiglow.domain.routine.dto.response.adaptation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "적응 액션 타입")
public enum AdaptationAction {
    
    @Schema(description = "목표 증가")
    INCREASE,
    
    @Schema(description = "목표 감소")
    DECREASE,
    
    @Schema(description = "성장 주기 리셋")
    RESET
}