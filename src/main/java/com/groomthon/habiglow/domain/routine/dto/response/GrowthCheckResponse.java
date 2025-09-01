package com.groomthon.habiglow.domain.routine.dto.response;

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
@Schema(description = "성장 주기 완료 루틴 목록 응답")
public class GrowthCheckResponse {
    
    @Schema(description = "성장 가능한 루틴 목록")
    private List<GrowthReadyRoutineResponse> growthReadyRoutines;
    
    @Schema(description = "성장 가능한 루틴 총 개수", example = "2")
    private Integer totalGrowthReadyCount;
}