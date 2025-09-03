package com.groomthon.habiglow.domain.routine.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineAdaptationCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ReductionReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.routine.service.strategy.ReductionStrategy;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReductionAnalysisService {

    private final Clock clock;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineRepository routineRepository;
    private final ReductionStrategy reductionStrategy;

    @Transactional(readOnly = true)
    public RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> analyzeReductionReadyRoutines(Long memberId) {
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);

        if (growthRoutines.isEmpty()) {
            return RoutineAdaptationCheckResponse.reduction(Collections.emptyList());
        }

        LocalDate endDate = LocalDate.now(clock).minusDays(1);

        List<ReductionReadyRoutineResponse> reductionReadyRoutines = growthRoutines.stream()
            .filter(routine -> {
                LocalDate startDate = endDate.minusDays(routine.getGrowthCycleDays() - 1);
                List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
                    .findByRoutineAndMemberAndDateRange(routine.getRoutineId(), memberId, startDate, endDate);
                
                log.info("Checking reduction for routine {}: period={} to {}, records={}", 
                    routine.getRoutineId(), startDate, endDate, recentRecords.size());
                
                boolean canAdapt = reductionStrategy.canAdapt(routine);
                boolean cycleCompleted = reductionStrategy.isAdaptationCycleCompleted(routine, recentRecords);
                
                log.info("Routine {}: canAdapt={}, cycleCompleted={}", routine.getRoutineId(), canAdapt, cycleCompleted);

                return canAdapt && cycleCompleted;
            })
            .map(routine -> {
                Integer suggestedTarget = reductionStrategy.calculateNewTargetValue(routine);
                LocalDate lastAttemptDate = findLastAttemptDate(routine.getRoutineId(), memberId, endDate);
                return ReductionReadyRoutineResponse.from(routine, suggestedTarget, lastAttemptDate);
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

        LocalDate endDate = LocalDate.now(clock).minusDays(1);
        LocalDate startDate = endDate.minusDays(routine.getGrowthCycleDays() - 1);

        List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
            .findByRoutineAndMemberAndDateRange(routine.getRoutineId(), memberId, startDate, endDate);

        if (!reductionStrategy.isAdaptationCycleCompleted(routine, recentRecords)) {
            throw new BaseException(ErrorCode.REDUCTION_CYCLE_NOT_COMPLETED);
        }
    }

    private LocalDate findLastAttemptDate(Long routineId, Long memberId, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(30);

        List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
            .findByRoutineAndMemberAndDateRange(routineId, memberId, startDate, endDate);

        return recentRecords.stream()
            .map(DailyRoutineEntity::getPerformedDate)
            .max(LocalDate::compareTo)
            .orElse(null);
    }
}