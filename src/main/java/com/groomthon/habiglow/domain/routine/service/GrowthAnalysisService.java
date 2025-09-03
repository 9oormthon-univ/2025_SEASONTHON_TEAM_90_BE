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
import com.groomthon.habiglow.domain.routine.dto.response.RoutineAdaptationCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.routine.service.strategy.GrowthStrategy;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthAnalysisService {

    private final Clock clock;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineRepository routineRepository;
    private final GrowthStrategy growthStrategy;

    @Transactional(readOnly = true)
    public RoutineAdaptationCheckResponse<GrowthReadyRoutineResponse> analyzeGrowthReadyRoutines(Long memberId) {
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);

        if (growthRoutines.isEmpty()) {
            return RoutineAdaptationCheckResponse.growth(Collections.emptyList());
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
                List<DailyRoutineEntity> records = lastRecord != null ? List.of(lastRecord) : List.of();
                return growthStrategy.canAdapt(routine) && 
                       growthStrategy.isAdaptationCycleCompleted(routine, records);
            })
            .map(routine -> {
                DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                return GrowthReadyRoutineResponse.from(routine, lastRecord);
            })
            .toList();

        log.info("Growth analysis completed for member: {}, found {} growth-ready routines",
            memberId, growthReadyRoutines.size());

        return RoutineAdaptationCheckResponse.growth(growthReadyRoutines);
    }

    public boolean isGrowthCycleCompleted(Long routineId, Long memberId, LocalDate targetDate) {
        Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
            .findSuccessRecordByRoutineAndMemberAndDate(routineId, memberId, targetDate, PerformanceLevel.FULL_SUCCESS);

        if (lastRecord.isEmpty()) {
            return false;
        }

        RoutineEntity routine = routineRepository.findById(routineId)
            .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));

        return growthStrategy.isAdaptationCycleCompleted(routine, List.of(lastRecord.get()));
    }

    public void validateGrowthConditions(RoutineEntity routine, Long memberId) {
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