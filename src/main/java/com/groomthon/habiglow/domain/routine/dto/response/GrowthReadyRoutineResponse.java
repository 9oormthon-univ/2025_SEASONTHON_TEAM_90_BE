package com.groomthon.habiglow.domain.routine.dto.response;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
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
@Schema(description = "성장 가능한 루틴 상세 정보")
public class GrowthReadyRoutineResponse {
    
    @Schema(description = "루틴 ID", example = "1")
    private Long routineId;
    
    @Schema(description = "루틴 제목", example = "매일 운동하기")
    private String title;
    
    @Schema(description = "루틴 카테고리", example = "HEALTH")
    private RoutineCategory category;
    
    @Schema(description = "목표 타입", example = "NUMBER")
    private TargetType targetType;
    
    @Schema(description = "현재 목표치", example = "20")
    private Integer currentTarget;
    
    @Schema(description = "다음 목표치", example = "25")
    private Integer nextTarget;
    
    @Schema(description = "증가량", example = "5")
    private Integer increment;
    
    @Schema(description = "완료된 성장 주기(일)", example = "7")
    private Integer completedCycleDays;
    
    @Schema(description = "연속 성공일", example = "7")
    private Integer consecutiveDays;
    
    @Schema(description = "현재 주기 연속일", example = "3")
    private Integer currentCycleDays;
    
    @Schema(description = "마지막 수행 날짜", example = "2025-01-30")
    private LocalDate lastPerformedDate;
    
    public static GrowthReadyRoutineResponse from(RoutineEntity routine, DailyRoutineEntity lastRecord) {
        return GrowthReadyRoutineResponse.builder()
                .routineId(routine.getRoutineId())
                .title(routine.getTitle())
                .category(routine.getCategory())
                .targetType(routine.getTargetType())
                .currentTarget(routine.getTargetValue())
                .nextTarget(routine.getTargetValue() + routine.getTargetIncrement())
                .increment(routine.getTargetIncrement())
                .completedCycleDays(routine.getGrowthCycleDays())
                .consecutiveDays(lastRecord.getConsecutiveDays())
                .currentCycleDays(routine.getCurrentCycleDays())
                .lastPerformedDate(lastRecord.getPerformedDate())
                .build();
    }
}