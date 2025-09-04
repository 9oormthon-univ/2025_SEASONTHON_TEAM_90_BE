package com.groomthon.habiglow.domain.routine.service.strategy;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.service.GrowthConfigurationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GrowthStrategy implements AdaptationStrategy {
    
    private final GrowthConfigurationService growthConfigService;

    @Override
    public boolean canAdapt(RoutineEntity routine) {
        return routine.isGrowthModeEnabled() && growthConfigService.canIncreaseTarget(routine);
    }

    @Override
    public boolean isAdaptationCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
        if (recentRecords == null || recentRecords.isEmpty()) {
            return false;
        }
        
        return routine.getGrowthConfiguration().isCycleCompleted();
    }

    @Override
    public Integer calculateNewTargetValue(RoutineEntity routine) {
        // 새로운 서비스에서 계산된 값 반환
        return routine.getTargetValue() + (routine.getTargetIncrement() != null ? routine.getTargetIncrement() : 0);
    }

    @Override
    public void executeAdaptation(RoutineEntity routine, Integer newTargetValue) {
        growthConfigService.executeTargetIncrease(routine);
    }
}