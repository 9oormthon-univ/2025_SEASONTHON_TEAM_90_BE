package com.groomthon.habiglow.domain.routine.service;

import org.springframework.stereotype.Service;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.common.TargetType;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoutineValidationService {

    public void validateCreateRequest(String title, RoutineCategory category, Boolean isGrowthMode,
                                     TargetType targetType, Integer targetValue,
                                     Integer growthCycleDays, Integer targetIncrement) {
        validateBasicInfo(title, category);
        
        if (Boolean.TRUE.equals(isGrowthMode)) {
            validateGrowthModeSettings(targetType, targetValue, growthCycleDays, targetIncrement);
        }
    }

    public void validateUpdateRequest(String description, RoutineCategory category, Boolean isGrowthMode,
                                     TargetType targetType, Integer targetValue,
                                     Integer growthCycleDays, Integer targetIncrement) {
        validateCategory(category);
        
        if (Boolean.TRUE.equals(isGrowthMode)) {
            validateGrowthModeSettings(targetType, targetValue, growthCycleDays, targetIncrement);
        }
    }

    private void validateBasicInfo(String title, RoutineCategory category) {
        validateTitle(title);
        validateCategory(category);
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty() || title.length() > 100) {
            throw new BaseException(ErrorCode.ROUTINE_INVALID_TITLE);
        }
    }

    private void validateCategory(RoutineCategory category) {
        if (category == null) {
            throw new BaseException(ErrorCode.ROUTINE_INVALID_CATEGORY);
        }
    }

    private void validateGrowthModeSettings(TargetType targetType, Integer targetValue,
                                           Integer growthCycleDays, Integer targetIncrement) {
        if (targetType == null || 
            targetValue == null || targetValue <= 0 ||
            growthCycleDays == null || growthCycleDays <= 0 ||
            targetIncrement == null || targetIncrement < 0) {

            throw new BaseException(ErrorCode.ROUTINE_INVALID_GROWTH_SETTINGS);
        }
    }
}