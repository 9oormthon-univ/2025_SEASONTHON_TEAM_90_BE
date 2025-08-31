package com.groomthon.habiglow.domain.daily.dto.response;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.TargetType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoutineRecordResponse {
    
    private Long routineId;
    private String routineTitle;
    private RoutineCategory category;
    private PerformanceLevel performanceLevel;
    private Integer consecutiveDays;
    
    // 성장 모드 스냅샷 정보
    private Boolean isGrowthMode;
    private TargetType targetType;
    private Integer targetValue;
    private Integer growthCycleDays;
    private Integer targetIncrement;
    
    public static RoutineRecordResponse from(DailyRoutineEntity entity) {
        return new RoutineRecordResponse(
            entity.getRoutine() != null ? entity.getRoutine().getRoutineId() : null,
            entity.getRoutineTitle(),
            entity.getRoutineCategory(),
            entity.getPerformanceLevel(),
            entity.getConsecutiveDays(),
            entity.getIsGrowthMode(),
            entity.getTargetType(),
            entity.getTargetValue(),
            entity.getGrowthCycleDays(),
            entity.getTargetIncrement()
        );
    }
}