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
public class ReductionStrategy implements AdaptationStrategy {
    
    private final GrowthConfigurationService growthConfigService;

    @Override
    public boolean canAdapt(RoutineEntity routine) {
        return growthConfigService.canDecreaseTarget(routine);
    }

    @Override
    public boolean isAdaptationCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
        if (!routine.isGrowthModeEnabled() || recentRecords == null || recentRecords.isEmpty()) {
            return false;
        }

        Integer cycleDays = routine.getGrowthCycleDays();
        LocalDate lastAdjustedDate = routine.getGrowthSettings().getLastAdjustedDate();

        List<DailyRoutineEntity> relevantRecords = recentRecords.stream()
            .filter(record -> lastAdjustedDate == null || record.getPerformedDate().isAfter(lastAdjustedDate))
            .toList();

        if (relevantRecords.size() < cycleDays) {
            return false;
        }

        boolean hasAnySuccess = relevantRecords.stream()
            .anyMatch(record -> record.getPerformanceLevel() == PerformanceLevel.FULL_SUCCESS);

        return !hasAnySuccess;
    }

    @Override
    public Integer calculateNewTargetValue(RoutineEntity routine) {
        // 서비스에서 계산 로직 사용
        Integer currentTarget = routine.getTargetValue();
        Integer decrement = routine.getGrowthSettings().getTargetDecrement();
        Integer minimumTarget = routine.getGrowthSettings().getMinimumTargetValue();

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