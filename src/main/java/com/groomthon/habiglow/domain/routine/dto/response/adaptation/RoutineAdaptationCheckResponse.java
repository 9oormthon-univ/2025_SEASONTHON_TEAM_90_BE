package com.groomthon.habiglow.domain.routine.dto.response.adaptation;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "루틴 적응 대상 조회 통합 응답")
public class RoutineAdaptationCheckResponse<T> {
    
    @Schema(description = "적응 대상 루틴 목록")
    private List<T> candidates;
    
    @Schema(description = "총 대상 개수")
    private Integer totalCount;
    
    @Schema(description = "적응 타입")
    private AdaptationType type;
    
    public static <T> RoutineAdaptationCheckResponse<T> of(List<T> candidates, AdaptationType type) {
        return RoutineAdaptationCheckResponse.<T>builder()
            .candidates(candidates)
            .totalCount(candidates.size())
            .type(type)
            .build();
    }
    
    public static RoutineAdaptationCheckResponse<GrowthReadyRoutineResponse> growth(List<GrowthReadyRoutineResponse> growthRoutines) {
        return of(growthRoutines, AdaptationType.GROWTH);
    }
    
    public static RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> reduction(List<ReductionReadyRoutineResponse> reductionRoutines) {
        return of(reductionRoutines, AdaptationType.REDUCTION);
    }
}