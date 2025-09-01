package com.groomthon.habiglow.domain.routine.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.IncreaseTargetResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ResetGrowthCycleResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.helper.RoutineHelper;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {
    
    private final Clock clock;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineRepository routineRepository;
    private final RoutineHelper routineHelper;

    @Transactional(readOnly = true)
    public GrowthCheckResponse checkGrowthReadyRoutines(Long memberId) {
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);
        
        if (growthRoutines.isEmpty()) {
            return GrowthCheckResponse.builder()
                    .growthReadyRoutines(Collections.emptyList())
                    .totalGrowthReadyCount(0)
                    .build();
        }

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        List<Long> routineIds = growthRoutines.stream()
                .map(RoutineEntity::getRoutineId)
                .toList();
        
        List<DailyRoutineEntity> yesterdayRecords = dailyRoutineRepository
                .findSuccessRecordsByRoutinesAndMemberAndDate(routineIds, memberId, yesterday, PerformanceLevel.FULL_SUCCESS);
        
        Map<Long, DailyRoutineEntity> recordMap = yesterdayRecords.stream()
                .collect(Collectors.toMap(
                        record -> record.getRoutine().getRoutineId(),
                        record -> record
                ));

        List<GrowthReadyRoutineResponse> growthReadyRoutines = growthRoutines.stream()
                .filter(routine -> {
                    DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                    return isGrowthCycleCompleted(routine, lastRecord);
                })
                .map(routine -> {
                    DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                    return GrowthReadyRoutineResponse.from(routine, lastRecord);
                })
                .toList();
        
        log.info("Growth check completed for member: {}, found {} growth-ready routines", 
                memberId, growthReadyRoutines.size());
        
        return GrowthCheckResponse.builder()
                .growthReadyRoutines(growthReadyRoutines)
                .totalGrowthReadyCount(growthReadyRoutines.size())
                .build();
    }

    
    @Transactional
    public IncreaseTargetResponse increaseRoutineTarget(Long routineId, Long memberId) {
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);

        validateGrowthConditions(routine, memberId);

        Integer previousTarget = routine.getTargetValue();
        routine.increaseTarget();

        log.info("Target increased for routine: {} from {} to {} by member: {}", 
                routineId, previousTarget, routine.getTargetValue(), memberId);

        return IncreaseTargetResponse.from(routine, previousTarget);
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
        Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
                .findSuccessRecordByRoutineAndMemberAndDate(routineId, memberId, targetDate, PerformanceLevel.FULL_SUCCESS);
                
        if (lastRecord.isEmpty()) {
            return false;
        }
        
        RoutineEntity routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
                
        return isGrowthCycleCompleted(routine, lastRecord.get());
    }

    private boolean isGrowthCycleCompleted(RoutineEntity routine, DailyRoutineEntity lastRecord) {

        if (lastRecord == null) {
            return false;
        }

        return routine.getGrowthSettings().isGrowthCycleCompleted();
    }
    
    private void validateGrowthConditions(RoutineEntity routine, Long memberId) {

        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }

        if (!routine.canIncreaseTarget()) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_INCREASE_TARGET);
        }

        if (!routine.getGrowthSettings().isGrowthCycleCompleted()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
                .findSuccessRecordByRoutineAndMemberAndDate(routine.getRoutineId(), memberId, yesterday, PerformanceLevel.FULL_SUCCESS);
        
        if (lastRecord.isEmpty()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }
    }
}