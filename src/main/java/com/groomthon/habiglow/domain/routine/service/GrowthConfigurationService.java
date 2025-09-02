package com.groomthon.habiglow.domain.routine.service;

import org.springframework.stereotype.Service;

import com.groomthon.habiglow.domain.routine.entity.GrowthConfiguration;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.entity.TargetType;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * GrowthConfiguration의 비즈니스 로직을 담당하는 도메인 서비스
 * 기존 GrowthSettings에 있던 복잡한 로직을 여기로 이동
 */
@Service
@RequiredArgsConstructor
public class GrowthConfigurationService {

    public boolean canIncreaseTarget(RoutineEntity routine) {
        GrowthConfiguration config = routine.getGrowthConfiguration();
        return config.isEnabled() && 
               config.getTargetValue() != null && 
               config.getTargetIncrement() != null && 
               config.getTargetIncrement() > 0;
    }

    public boolean canDecreaseTarget(RoutineEntity routine) {
        GrowthConfiguration config = routine.getGrowthConfiguration();
        
        if (!config.isEnabled()) {
            return false;
        }

        Integer currentTarget = config.getTargetValue();
        Integer minimumTarget = config.getMinimumTargetValue() != null ? config.getMinimumTargetValue() : 1;

        return currentTarget != null && currentTarget > minimumTarget;
    }

    public Integer executeTargetIncrease(RoutineEntity routine) {
        GrowthConfiguration config = routine.getGrowthConfiguration();
        
        if (!canIncreaseTarget(routine)) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_INCREASE_TARGET);
        }

        Integer newTarget = config.getTargetValue() + config.getTargetIncrement();
        
        // 새로운 설정으로 업데이트 (불변성 유지)
        GrowthConfiguration updatedConfig = config.updateTargetValue(newTarget);
        routine.updateGrowthConfiguration(updatedConfig);
        
        return newTarget;
    }

    public Integer executeTargetDecrease(RoutineEntity routine) {
        GrowthConfiguration config = routine.getGrowthConfiguration();
        
        if (!canDecreaseTarget(routine)) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_DECREASE_TARGET);
        }

        Integer currentTarget = config.getTargetValue();
        Integer decrement = config.getTargetDecrement();
        Integer minimumTarget = config.getMinimumTargetValue() != null ? config.getMinimumTargetValue() : 1;

        if (decrement == null) {
            decrement = Math.max(1, config.getTargetIncrement() / 2);
        }

        Integer newTarget = Math.max(currentTarget - decrement, minimumTarget);
        
        // 새로운 설정으로 업데이트 (불변성 유지)
        GrowthConfiguration updatedConfig = config.updateTargetValue(newTarget);
        routine.updateGrowthConfiguration(updatedConfig);
        
        return newTarget;
    }

    public void resetGrowthCycle(RoutineEntity routine) {
        GrowthConfiguration config = routine.getGrowthConfiguration();
        
        if (!config.isEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }

        if (!config.isCycleCompleted()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }

        GrowthConfiguration updatedConfig = config.resetCurrentCycleDays();
        routine.updateGrowthConfiguration(updatedConfig);
    }

    public void incrementCycleDays(RoutineEntity routine) {
        GrowthConfiguration config = routine.getGrowthConfiguration();
        
        if (config.isEnabled()) {
            GrowthConfiguration updatedConfig = config.incrementCurrentCycleDays();
            routine.updateGrowthConfiguration(updatedConfig);
        }
    }

    public GrowthConfiguration createGrowthConfiguration(Boolean isGrowthMode, TargetType targetType, 
            Integer targetValue, Integer growthCycleDays, Integer targetIncrement) {
        
        if (Boolean.TRUE.equals(isGrowthMode)) {
            return GrowthConfiguration.of(targetType, targetValue, growthCycleDays, targetIncrement);
        } else {
            return GrowthConfiguration.disabled();
        }
    }
}