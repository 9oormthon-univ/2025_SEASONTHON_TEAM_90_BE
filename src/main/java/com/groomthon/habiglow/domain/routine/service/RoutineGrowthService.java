package com.groomthon.habiglow.domain.routine.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.response.GrowthAnalysisResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoutineGrowthService {

    @Transactional
    public void processTargetIncrease(RoutineEntity routine) {
        if (!routine.isGrowthModeEnabled()) {
            log.debug("Growth mode is disabled for routine: {}", routine.getRoutineId());
            return;
        }

        if (!routine.canIncreaseTarget()) {
            log.warn("Cannot increase target for routine: {}. Growth conditions not met", routine.getRoutineId());
            return;
        }

        Integer previousTarget = routine.getTargetValue();
        routine.increaseTarget();
        Integer newTarget = routine.getTargetValue();

        log.info("Target increased for routine: {} from {} to {}", 
                routine.getRoutineId(), previousTarget, newTarget);
    }

    private String getGrowthProgressMessage(RoutineEntity routine) {
        if (!routine.isGrowthModeEnabled()) {
            return "성장 모드가 비활성화되어 있습니다.";
        }

        Integer currentTarget = routine.getTargetValue();
        Integer increment = routine.getTargetIncrement();
        
        return String.format("현재 목표: %d, 다음 목표: %d (증가량: +%d)", 
                currentTarget, currentTarget + increment, increment);
    }

    @Transactional(readOnly = true)
    public GrowthAnalysisResponse analyzeGrowthPotential(RoutineEntity routine) {
        return GrowthAnalysisResponse.builder()
                .routineId(routine.getRoutineId())
                .isGrowthEnabled(routine.isGrowthModeEnabled())
                .canIncrease(routine.canIncreaseTarget())
                .currentTarget(routine.getTargetValue())
                .increment(routine.getTargetIncrement())
                .cycleDays(routine.getGrowthCycleDays())
                .targetType(routine.getTargetType())
                .progressMessage(getGrowthProgressMessage(routine))
                .build();
    }
}