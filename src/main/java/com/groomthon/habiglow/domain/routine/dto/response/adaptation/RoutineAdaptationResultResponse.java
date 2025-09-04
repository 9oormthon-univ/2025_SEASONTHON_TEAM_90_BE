package com.groomthon.habiglow.domain.routine.dto.response.adaptation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "루틴 적응 실행 결과 응답")
public class RoutineAdaptationResultResponse {
    
    @Schema(description = "루틴 ID")
    private Long routineId;
    
    @Schema(description = "루틴 제목")
    private String routineTitle;
    
    @Schema(description = "이전 목표값")
    private Integer previousValue;
    
    @Schema(description = "새로운 목표값")
    private Integer newValue;
    
    @Schema(description = "적응 액션")
    private AdaptationAction action;
    
    @Schema(description = "적응 성공 여부")
    private Boolean success;
    
    @Schema(description = "메시지")
    private String message;
    
    public static RoutineAdaptationResultResponse success(Long routineId, String title, Integer previous, Integer current, AdaptationAction action) {
        return RoutineAdaptationResultResponse.builder()
            .routineId(routineId)
            .routineTitle(title)
            .previousValue(previous)
            .newValue(current)
            .action(action)
            .success(true)
            .message(action + " 성공")
            .build();
    }
    
    public static RoutineAdaptationResultResponse failure(Long routineId, String title, String errorMessage) {
        return RoutineAdaptationResultResponse.builder()
            .routineId(routineId)
            .routineTitle(title)
            .success(false)
            .message(errorMessage)
            .build();
    }
}