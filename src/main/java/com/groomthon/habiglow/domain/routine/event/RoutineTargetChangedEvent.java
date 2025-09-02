package com.groomthon.habiglow.domain.routine.event;

import java.time.LocalDateTime;

import com.groomthon.habiglow.domain.routine.dto.response.AdaptationAction;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoutineTargetChangedEvent {
    
    private final Long routineId;
    private final String routineTitle;
    private final Long memberId;
    private final Integer previousTarget;
    private final Integer newTarget;
    private final AdaptationAction action;
    private final LocalDateTime occurredAt;
    
    public static RoutineTargetChangedEvent of(Long routineId, String routineTitle, Long memberId, 
                                               Integer previousTarget, Integer newTarget, AdaptationAction action) {
        return new RoutineTargetChangedEvent(
            routineId, routineTitle, memberId, 
            previousTarget, newTarget, action, LocalDateTime.now()
        );
    }
    
    public Integer getTargetChange() {
        if (action == AdaptationAction.INCREASE) {
            return newTarget - previousTarget;
        } else if (action == AdaptationAction.DECREASE) {
            return previousTarget - newTarget;
        } else {
            return 0; // RESET의 경우
        }
    }
}