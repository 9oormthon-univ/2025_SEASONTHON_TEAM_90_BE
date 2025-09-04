package com.groomthon.habiglow.domain.routine.service.strategy;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.service.GrowthConfigurationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReductionStrategy implements AdaptationStrategy {
    
    private final GrowthConfigurationService growthConfigService;

    @Override
    public boolean canAdapt(RoutineEntity routine) {
        return growthConfigService.canDecreaseTarget(routine);
    }

    @Override
    public boolean isAdaptationCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
        if (!routine.isGrowthModeEnabled()) {
            log.info("Routine {} is not in growth mode", routine.getRoutineId());
            return false;
        }
        
        // 단순화된 로직: 실패 카운트만 체크
        boolean isFailureCycleCompleted = routine.getGrowthConfiguration().isFailureCycleCompleted();
        
        log.info("Reduction check for routine {}: failureCycleDays={}, growthCycleDays={}, completed={}", 
            routine.getRoutineId(), 
            routine.getGrowthConfiguration().getFailureCycleDays(),
            routine.getGrowthCycleDays(),
            isFailureCycleCompleted);
        
        return isFailureCycleCompleted;
    }

    @Override
    public Integer calculateNewTargetValue(RoutineEntity routine) {
        // 서비스에서 계산 로직 사용
        Integer currentTarget = routine.getTargetValue();
        Integer decrement = routine.getGrowthConfiguration().getTargetDecrement();
        Integer minimumTarget = routine.getGrowthConfiguration().getMinimumTargetValue();

        if (decrement == null) {
            decrement = Math.max(1, routine.getTargetIncrement() / 2);
        }

        Integer newTarget = currentTarget - decrement;
        return Math.max(newTarget, minimumTarget != null ? minimumTarget : 1);
    }

    @Override
    public void executeAdaptation(RoutineEntity routine, Integer newTargetValue) {
        growthConfigService.executeTargetDecrease(routine);
    }
}