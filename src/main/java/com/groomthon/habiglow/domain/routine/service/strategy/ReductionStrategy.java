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
        
        if (recentRecords == null || recentRecords.isEmpty()) {
            log.info("No recent records for routine {}", routine.getRoutineId());
            return false;
        }

        Integer cycleDays = routine.getGrowthCycleDays();
        LocalDate lastAdjustedDate = routine.getGrowthConfiguration().getLastAdjustedDate();
        
        log.info("Reduction check for routine {}: cycleDays={}, lastAdjustedDate={}, recentRecords={}",
            routine.getRoutineId(), cycleDays, lastAdjustedDate, recentRecords.size());

        List<DailyRoutineEntity> relevantRecords = recentRecords.stream()
            .filter(record -> lastAdjustedDate == null || record.getPerformedDate().isAfter(lastAdjustedDate))
            .toList();
        
        log.info("Relevant records for routine {}: {}", routine.getRoutineId(), relevantRecords.size());

        // 관련 기록이 없으면 감소 불가 (최근에 조정되어 평가할 기록이 없음)
        if (relevantRecords.isEmpty()) {
            log.info("No relevant records after lastAdjustedDate for routine {}", routine.getRoutineId());
            return false;
        }

        // 성장 주기 동안 FULL_SUCCESS가 있는지 확인
        // 기록이 없는 날은 자동으로 NOT_PERFORMED = 실패로 간주됨
        boolean hasAnySuccess = relevantRecords.stream()
            .anyMatch(record -> record.getPerformanceLevel() == PerformanceLevel.FULL_SUCCESS);
        
        log.info("Routine {} has success in cycle: {}, should reduce: {}", routine.getRoutineId(), hasAnySuccess, !hasAnySuccess);

        return !hasAnySuccess;
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