package com.groomthon.habiglow.domain.routine.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.response.AdaptationAction;
import com.groomthon.habiglow.domain.routine.dto.response.AdaptiveRoutineCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ReductionReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineAdaptationCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ResetGrowthCycleResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineAdaptationResultResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.helper.RoutineHelper;
import com.groomthon.habiglow.domain.routine.facade.RoutineManagementFacade;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {

    private final RoutineHelper routineHelper;
    private final GrowthAnalysisService growthAnalysisService;
    private final ReductionAnalysisService reductionAnalysisService;
    private final RoutineManagementFacade routineManagementFacade;

    @Transactional(readOnly = true)
    public RoutineAdaptationCheckResponse<GrowthReadyRoutineResponse> checkGrowthReadyRoutines(Long memberId) {
        return growthAnalysisService.analyzeGrowthReadyRoutines(memberId);
    }


    @Transactional
    public RoutineAdaptationResultResponse increaseRoutineTarget(Long routineId, Long memberId) {
        return routineManagementFacade.executeRoutineAdaptation(memberId, routineId, AdaptationAction.INCREASE);
    }

    @Transactional
    public ResetGrowthCycleResponse resetGrowthCycle(Long routineId, Long memberId) {
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);

        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }

        if (!routine.getGrowthSettings().isGrowthCycleCompleted()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }

        Integer previousCycleDays = routine.getCurrentCycleDays();

        routine.getGrowthSettings().resetCurrentCycleDays();

        log.info("Growth cycle reset for routine: {} by member: {}, previous cycle days: {}",
            routineId, memberId, previousCycleDays);

        return ResetGrowthCycleResponse.from(routine, previousCycleDays);
    }


    public boolean isGrowthCycleCompleted(Long routineId, Long memberId, LocalDate targetDate) {
        return growthAnalysisService.isGrowthCycleCompleted(routineId, memberId, targetDate);
    }

    @Transactional(readOnly = true)
    public AdaptiveRoutineCheckResponse checkAdaptiveRoutines(Long memberId) {
        RoutineAdaptationCheckResponse<GrowthReadyRoutineResponse> growthResponse = growthAnalysisService.analyzeGrowthReadyRoutines(memberId);
        RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> reductionResponse = reductionAnalysisService.analyzeReductionReadyRoutines(memberId);

        List<GrowthReadyRoutineResponse> growthReadyRoutines = growthResponse.getCandidates();
        
        Set<Long> growthReadyIds = growthReadyRoutines.stream()
            .map(GrowthReadyRoutineResponse::getRoutineId)
            .collect(Collectors.toSet());

        List<ReductionReadyRoutineResponse> filteredReductionRoutines = 
            reductionResponse.getCandidates().stream()
            .filter(routine -> !growthReadyIds.contains(routine.getRoutineId()))
            .toList();

        log.info("Adaptive check completed for member: {}, found {} growth-ready and {} reduction-ready routines",
            memberId, growthReadyRoutines.size(), filteredReductionRoutines.size());

        return AdaptiveRoutineCheckResponse.of(growthReadyRoutines, filteredReductionRoutines);
    }

    @Transactional(readOnly = true)
    public RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> checkReductionReadyRoutines(Long memberId) {
        return reductionAnalysisService.analyzeReductionReadyRoutines(memberId);
    }

    @Transactional
    public RoutineAdaptationResultResponse decreaseRoutineTarget(Long routineId, Long memberId) {
        return routineManagementFacade.executeRoutineAdaptation(memberId, routineId, AdaptationAction.DECREASE);
    }

}