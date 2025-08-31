package com.groomthon.habiglow.domain.daily.dto.request;

import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoutinePerformanceRequest {
    
    private RoutineEntity routine;
    private MemberEntity member;
    private PerformanceLevel performanceLevel;
    
    public static RoutinePerformanceRequest of(RoutineEntity routine, MemberEntity member, PerformanceLevel performanceLevel) {
        return new RoutinePerformanceRequest(routine, member, performanceLevel);
    }
    
    public Long getRoutineId() {
        return routine.getRoutineId();
    }
}