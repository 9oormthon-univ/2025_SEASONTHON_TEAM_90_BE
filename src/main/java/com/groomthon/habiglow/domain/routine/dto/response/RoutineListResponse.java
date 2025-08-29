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
@Schema(description = "루틴 목록 응답")
public class RoutineListResponse {
    
    @Schema(description = "루틴 목록")
    private List<RoutineResponse> routines;
    
    @Schema(description = "총 개수", example = "5")
    private Integer totalCount;
    
    public static RoutineListResponse of(List<RoutineResponse> routines) {
        return RoutineListResponse.builder()
                .routines(routines)
                .totalCount(routines.size())
                .build();
    }
}