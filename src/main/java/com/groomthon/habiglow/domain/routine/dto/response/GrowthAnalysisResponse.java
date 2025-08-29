package com.groomthon.habiglow.domain.routine.dto.response;

import com.groomthon.habiglow.domain.routine.entity.TargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "루틴 성장 분석 응답")
public class GrowthAnalysisResponse {
    
    @Schema(description = "루틴 ID", example = "1")
    private Long routineId;
    
    @Schema(description = "성장 모드 활성화 여부", example = "true")
    private boolean isGrowthEnabled;
    
    @Schema(description = "목표 증가 가능 여부", example = "true")
    private boolean canIncrease;
    
    @Schema(description = "현재 목표값", example = "500")
    private Integer currentTarget;
    
    @Schema(description = "증가량", example = "50")
    private Integer increment;
    
    @Schema(description = "성장 주기(일)", example = "7")
    private Integer cycleDays;
    
    @Schema(description = "목표 타입", example = "NUMBER")
    private TargetType targetType;
    
    @Schema(description = "진행 상황 메시지", example = "현재 목표: 500, 다음 목표: 550 (증가량: +50)")
    private String progressMessage;
}