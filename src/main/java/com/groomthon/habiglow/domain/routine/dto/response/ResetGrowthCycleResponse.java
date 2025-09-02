package com.groomthon.habiglow.domain.routine.dto.response;

import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
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
@Schema(description = "성장 주기 리셋 결과 응답")
public class ResetGrowthCycleResponse {
    
    @Schema(description = "루틴 ID", example = "1")
    private Long routineId;
    
    @Schema(description = "루틴 제목", example = "매일 팔굽혀펴기")
    private String title;
    
    @Schema(description = "현재 목표치", example = "10")
    private Integer currentTarget;
    
    @Schema(description = "목표 타입", example = "NUMBER")
    private TargetType targetType;
    
    @Schema(description = "성장 주기 (일)", example = "7")
    private Integer growthCycleDays;
    
    @Schema(description = "리셋된 현재 주기일", example = "0")
    private Integer currentCycleDays;
    
    @Schema(description = "결과 메시지", example = "성장 주기가 리셋되었습니다. 새로운 주기를 시작하세요!")
    private String message;
    
    public static ResetGrowthCycleResponse from(RoutineEntity routine, Integer previousCycleDays) {
        String targetUnit = routine.getTargetType().getUnit();
        String message = String.format("성장 주기가 리셋되었습니다. %d%s 목표로 새로운 %d일 주기를 시작하세요!", 
                routine.getTargetValue(), targetUnit, routine.getGrowthCycleDays());
                
        return ResetGrowthCycleResponse.builder()
                .routineId(routine.getRoutineId())
                .title(routine.getTitle())
                .currentTarget(routine.getTargetValue())
                .targetType(routine.getTargetType())
                .growthCycleDays(routine.getGrowthCycleDays())
                .currentCycleDays(routine.getCurrentCycleDays())
                .message(message)
                .build();
    }
}