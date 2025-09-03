package com.groomthon.habiglow.domain.routine.event;

import java.time.LocalDateTime;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.common.TargetType;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 루틴 생성 이벤트
 * 새로운 루틴이 생성될 때 발생
 */
@Getter
@AllArgsConstructor
public class RoutineCreatedEvent {
    
    private final Long routineId;
    private final String routineTitle;
    private final Long memberId;
    private final RoutineCategory category;
    private final Boolean isGrowthMode;
    private final TargetType targetType;
    private final Integer targetValue;
    private final LocalDateTime occurredAt;
    
    public static RoutineCreatedEvent of(Long routineId, String routineTitle, Long memberId, 
                                        RoutineCategory category, Boolean isGrowthMode, 
                                        TargetType targetType, Integer targetValue) {
        return new RoutineCreatedEvent(
            routineId, routineTitle, memberId, category, 
            isGrowthMode, targetType, targetValue, LocalDateTime.now()
        );
    }
}