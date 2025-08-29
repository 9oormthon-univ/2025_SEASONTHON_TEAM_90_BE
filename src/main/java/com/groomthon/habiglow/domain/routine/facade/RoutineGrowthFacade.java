package com.groomthon.habiglow.domain.routine.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.response.GrowthAnalysisResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.helper.RoutineHelper;
import com.groomthon.habiglow.domain.routine.service.RoutineGrowthService;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 루틴 성장 모드 관리 Facade
 * 단순한 성장 모드 on/off와 목표치 증가 기능만 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineGrowthFacade {
    
    private final RoutineHelper routineHelper;
    private final RoutineGrowthService growthService;

    /**
     * 수동으로 목표치 증가
     */
    @Transactional
    public RoutineResponse forceTargetIncrease(Long memberId, Long routineId) {
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_GROWTH_MODE_DISABLED);
        }
        
        Integer previousTarget = routine.getTargetValue();
        growthService.processTargetIncrease(routine);
        Integer newTarget = routine.getTargetValue();
        
        log.info("Manual target increase for routine: {} by member: {} - {} → {}", 
                routineId, memberId, previousTarget, newTarget);
        
        return RoutineResponse.from(routine);
    }

    /**
     * 성장 모드 상태 분석
     */
    @Transactional(readOnly = true)
    public GrowthAnalysisResponse analyzeGrowthStatus(Long memberId, Long routineId) {
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        return growthService.analyzeGrowthPotential(routine);
    }
}