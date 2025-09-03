package com.groomthon.habiglow.domain.routine.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 성장 주기 완료 이벤트
 * 루틴의 성장 주기가 완료되어 목표 증가가 가능해질 때 발생
 */
@Getter
@AllArgsConstructor
public class GrowthCycleCompletedEvent {
    
    private final Long routineId;
    private final String routineTitle;
    private final Long memberId;
    private final Integer currentTarget;
    private final Integer cycleDays;
    private final Integer completedCycles;
    private final LocalDateTime occurredAt;
    
    public static GrowthCycleCompletedEvent of(Long routineId, String routineTitle, Long memberId, 
                                              Integer currentTarget, Integer cycleDays, 
                                              Integer completedCycles) {
        return new GrowthCycleCompletedEvent(
            routineId, routineTitle, memberId, currentTarget, 
            cycleDays, completedCycles, LocalDateTime.now()
        );
    }
}