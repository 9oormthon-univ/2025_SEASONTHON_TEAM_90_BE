package com.groomthon.habiglow.domain.routine.service.strategy;

import java.time.LocalDate;
import java.util.List;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

public interface AdaptationStrategy {
    
    boolean canAdapt(RoutineEntity routine);
    
    boolean isAdaptationCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords);
    
    Integer calculateNewTargetValue(RoutineEntity routine);
    
    void executeAdaptation(RoutineEntity routine, Integer newTargetValue);
    
    default LocalDate getStartDate(LocalDate endDate, Integer cycleDays) {
        return endDate.minusDays(cycleDays - 1);
    }
}