package com.groomthon.habiglow.domain.routine.dto.response;

import java.time.LocalDateTime;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.common.TargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "루틴 응답")
public class RoutineResponse {
    
    @Schema(description = "루틴 ID", example = "1")
    private Long routineId;
    
    @Schema(description = "루틴 카테고리", example = "HEALTH")
    private RoutineCategory category;
    
    @Schema(description = "루틴 제목", example = "매일 운동하기")
    private String title;
    
    @Schema(description = "루틴 설명", example = "건강한 생활을 위해 매일 30분씩 운동하기")
    private String description;
    
    @Schema(description = "성장 모드 활성화 여부", example = "true")
    private Boolean isGrowthMode;
    
    @Schema(description = "목표 타입", example = "NUMBER")
    private TargetType targetType;
    
    @Schema(description = "목표 수치", example = "500")
    private Integer targetValue;
    
    @Schema(description = "성장 주기(일)", example = "7")
    private Integer growthCycleDays;
    
    @Schema(description = "목표 증가 수치", example = "50")
    private Integer targetIncrement;
    
    @Schema(description = "현재 성공 주기 일수", example = "3")
    private Integer currentCycleDays;
    
    @Schema(description = "현재 실패 주기 일수", example = "2")
    private Integer failureCycleDays;
    
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;
    
    public static RoutineResponse from(RoutineEntity routine) {
        return RoutineResponse.builder()
                .routineId(routine.getRoutineId())
                .category(routine.getCategory())
                .title(routine.getTitle())
                .description(routine.getDescription())
                .isGrowthMode(routine.getIsGrowthMode())
                .targetType(routine.getTargetType())
                .targetValue(routine.getTargetValue())
                .growthCycleDays(routine.getGrowthCycleDays())
                .targetIncrement(routine.getTargetIncrement())
                .currentCycleDays(routine.getCurrentCycleDays())
                .failureCycleDays(routine.getGrowthConfiguration().getFailureCycleDays())
                .createdAt(routine.getCreatedAt())
                .updatedAt(routine.getUpdatedAt())
                .build();
    }
}