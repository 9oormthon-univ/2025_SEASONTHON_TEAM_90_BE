package com.groomthon.habiglow.domain.routine.event;

import java.time.LocalDateTime;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 루틴 삭제 이벤트
 * 루틴이 삭제될 때 발생 (정리 작업 트리거)
 */
@Getter
@AllArgsConstructor
public class RoutineDeletedEvent {
    
    private final Long routineId;
    private final String routineTitle;
    private final Long memberId;
    private final RoutineCategory category;
    private final Boolean wasGrowthEnabled;
    private final Integer finalTargetValue;
    private final LocalDateTime occurredAt;
    
    public static RoutineDeletedEvent of(Long routineId, String routineTitle, Long memberId, 
                                        RoutineCategory category, Boolean wasGrowthEnabled, 
                                        Integer finalTargetValue) {
        return new RoutineDeletedEvent(
            routineId, routineTitle, memberId, category, 
            wasGrowthEnabled, finalTargetValue, LocalDateTime.now()
        );
    }
}