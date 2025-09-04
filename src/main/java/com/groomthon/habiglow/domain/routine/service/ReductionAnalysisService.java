package com.groomthon.habiglow.domain.routine.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.response.adaptation.ReductionReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.RoutineAdaptationCheckResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.service.strategy.ReductionStrategy;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReductionAnalysisService {

    private final RoutineDataAccessService routineDataAccessService;
    private final ReductionStrategy reductionStrategy;

    @Transactional(readOnly = true)
    public RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> analyzeReductionReadyRoutines(Long memberId) {
        List<RoutineEntity> growthRoutines = routineDataAccessService.findGrowthEnabledRoutines(memberId);

        if (growthRoutines.isEmpty()) {
            return RoutineAdaptationCheckResponse.reduction(Collections.emptyList());
        }

        List<ReductionReadyRoutineResponse> reductionReadyRoutines = growthRoutines.stream()
            .filter(routine -> reductionStrategy.canAdapt(routine) && 
                              reductionStrategy.isAdaptationCycleCompleted(routine, Collections.emptyList()))
            .map(routine -> {
                Integer suggestedTarget = reductionStrategy.calculateNewTargetValue(routine);
                return ReductionReadyRoutineResponse.from(routine, suggestedTarget, null);
            })
            .toList();

        log.info("Reduction analysis completed for member: {}, found {} reduction-ready routines",
            memberId, reductionReadyRoutines.size());

        return RoutineAdaptationCheckResponse.reduction(reductionReadyRoutines);
    }

    public void validateReductionConditions(RoutineEntity routine, Long memberId) {
        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }

        if (!reductionStrategy.canAdapt(routine)) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_DECREASE_TARGET);
        }

        if (!reductionStrategy.isAdaptationCycleCompleted(routine, Collections.emptyList())) {
            throw new BaseException(ErrorCode.REDUCTION_CYCLE_NOT_COMPLETED);
        }
    }
}