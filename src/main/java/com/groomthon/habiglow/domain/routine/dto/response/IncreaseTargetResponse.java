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
@Schema(description = "목표치 증가 결과 응답")
public class IncreaseTargetResponse {
    
    @Schema(description = "루틴 ID", example = "1")
    private Long routineId;
    
    @Schema(description = "루틴 제목", example = "매일 운동하기")
    private String title;
    
    @Schema(description = "이전 목표치", example = "20")
    private Integer previousTarget;
    
    @Schema(description = "새로운 목표치", example = "25")
    private Integer newTarget;
    
    @Schema(description = "증가량", example = "5")
    private Integer increment;
    
    @Schema(description = "목표 타입", example = "NUMBER")
    private TargetType targetType;
    
    @Schema(description = "성공 메시지", example = "목표가 20개에서 25개로 증가되었습니다!")
    private String message;
    
    public static IncreaseTargetResponse from(RoutineEntity routine, Integer previousTarget) {
        String targetUnit = routine.getTargetType().getUnit();
        String message = String.format("목표가 %d%s에서 %d%s로 증가되었습니다!", 
                previousTarget, targetUnit, routine.getTargetValue(), targetUnit);
                
        return IncreaseTargetResponse.builder()
                .routineId(routine.getRoutineId())
                .title(routine.getTitle())
                .previousTarget(previousTarget)
                .newTarget(routine.getTargetValue())
                .increment(routine.getTargetIncrement())
                .targetType(routine.getTargetType())
                .message(message)
                .build();
    }
}