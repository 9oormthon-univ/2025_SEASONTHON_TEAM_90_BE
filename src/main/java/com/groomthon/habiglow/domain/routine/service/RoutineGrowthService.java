package com.groomthon.habiglow.domain.routine.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.dto.response.AdaptiveRoutineCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.DecreaseTargetResponse;
import com.groomthon.habiglow.domain.routine.dto.response.DifficultyReductionCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.IncreaseTargetResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ReductionReadyRoutineResponse;
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

    // ==================== 통합 적응형 조정 로직 ====================

    /**
     * 적응형 루틴 조정 대상 통합 조회 (성장 + 감소)
     */
    @Transactional(readOnly = true)
    public AdaptiveRoutineCheckResponse checkAdaptiveRoutines(Long memberId) {
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);

        if (growthRoutines.isEmpty()) {
            return AdaptiveRoutineCheckResponse.of(Collections.emptyList(), Collections.emptyList());
        }

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        // 성장 대상과 감소 대상을 동시에 검사
        List<GrowthReadyRoutineResponse> growthReadyRoutines = Collections.emptyList();
        List<ReductionReadyRoutineResponse> reductionReadyRoutines = Collections.emptyList();

        // 기존 성장 로직
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

        // 성장 대상 루틴 필터링
        growthReadyRoutines = growthRoutines.stream()
            .filter(routine -> {
                DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                return isGrowthCycleCompleted(routine, lastRecord);
            })
            .map(routine -> {
                DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                return GrowthReadyRoutineResponse.from(routine, lastRecord);
            })
            .toList();

        // 감소 대상 루틴 필터링 (성장 대상이 아닌 것들 중에서)
        LocalDate endDate = yesterday;
        Set<Long> growthReadyIds = growthReadyRoutines.stream()
            .map(GrowthReadyRoutineResponse::getRoutineId)
            .collect(Collectors.toSet());

        reductionReadyRoutines = growthRoutines.stream()
            .filter(routine -> !growthReadyIds.contains(routine.getRoutineId())) // 성장 대상 제외
            .filter(routine -> {
                LocalDate startDate = endDate.minusDays(routine.getGrowthCycleDays() - 1);
                List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
                    .findByRoutineAndMemberAndDateRange(routine.getRoutineId(), memberId, startDate, endDate);

                return canDecreaseTarget(routine) && isReductionCycleCompleted(routine, recentRecords);
            })
            .map(routine -> {
                Integer suggestedTarget = calculateNewTargetValue(routine);
                LocalDate lastAttemptDate = findLastAttemptDate(routine.getRoutineId(), memberId, endDate);
                return ReductionReadyRoutineResponse.from(routine, suggestedTarget, lastAttemptDate);
            })
            .toList();

        log.info("Adaptive check completed for member: {}, found {} growth-ready and {} reduction-ready routines",
            memberId, growthReadyRoutines.size(), reductionReadyRoutines.size());

        return AdaptiveRoutineCheckResponse.of(growthReadyRoutines, reductionReadyRoutines);
    }

    // ==================== 난이도 감소 로직 ====================

    /**
     * 감소 가능한 루틴 조회
     */
    @Transactional(readOnly = true)
    public DifficultyReductionCheckResponse checkReductionReadyRoutines(Long memberId) {
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);

        if (growthRoutines.isEmpty()) {
            return DifficultyReductionCheckResponse.of(Collections.emptyList());
        }

        LocalDate endDate = LocalDate.now(clock).minusDays(1);

        List<ReductionReadyRoutineResponse> reductionReadyRoutines = growthRoutines.stream()
            .filter(routine -> {
                LocalDate startDate = endDate.minusDays(routine.getGrowthCycleDays() - 1);
                List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
                    .findByRoutineAndMemberAndDateRange(routine.getRoutineId(), memberId, startDate, endDate);

                return canDecreaseTarget(routine) && isReductionCycleCompleted(routine, recentRecords);
            })
            .map(routine -> {
                Integer suggestedTarget = calculateNewTargetValue(routine);
                LocalDate lastAttemptDate = findLastAttemptDate(routine.getRoutineId(), memberId, endDate);
                return ReductionReadyRoutineResponse.from(routine, suggestedTarget, lastAttemptDate);
            })
            .toList();

        log.info("Reduction check completed for member: {}, found {} reduction-ready routines",
            memberId, reductionReadyRoutines.size());

        return DifficultyReductionCheckResponse.of(reductionReadyRoutines);
    }

    /**
     * 루틴 목표값 감소
     */
    @Transactional
    public DecreaseTargetResponse decreaseRoutineTarget(Long routineId, Long memberId) {
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);

        validateReductionConditions(routine, memberId);

        Integer previousTarget = routine.getTargetValue();
        Integer newTarget = calculateNewTargetValue(routine);

        routine.getGrowthSettings().decreaseTarget(newTarget);
        routine.getGrowthSettings().resetCurrentCycleDays();

        log.info("Target decreased for routine: {} from {} to {} by member: {}",
            routineId, previousTarget, newTarget, memberId);

        return DecreaseTargetResponse.from(routine, previousTarget);
    }

    /**
     * 감소 가능 여부 판단
     */
    private boolean canDecreaseTarget(RoutineEntity routine) {
        if (!routine.isGrowthModeEnabled()) {
            return false;
        }

        Integer currentTarget = routine.getTargetValue();
        Integer minimumTarget = routine.getGrowthSettings().getMinimumTargetValue();

        return currentTarget != null && minimumTarget != null && currentTarget > minimumTarget;
    }

    /**
     * 감소 주기 완료 여부 판단
     */
    private boolean isReductionCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
        if (!routine.isGrowthModeEnabled()) {
            return false;
        }

        Integer cycleDays = routine.getGrowthCycleDays();
        if (recentRecords.size() < cycleDays) {
            return false;
        }

        // 핵심 조건: 최근 cycleDays 동안 FULL_SUCCESS가 한 번도 없었는지만 확인
        boolean hasAnySuccess = recentRecords.stream()
            .anyMatch(record -> record.getPerformanceLevel() == PerformanceLevel.FULL_SUCCESS);

        return !hasAnySuccess; // 성공이 없으면 감소 주기 완료
    }

    /**
     * 새로운 목표값 계산
     */
    private Integer calculateNewTargetValue(RoutineEntity routine) {
        Integer currentTarget = routine.getTargetValue();
        Integer decrement = routine.getGrowthSettings().getTargetDecrement();
        Integer minimumTarget = routine.getGrowthSettings().getMinimumTargetValue();

        if (decrement == null) {
            // targetDecrement가 없으면 targetIncrement의 절반 사용
            decrement = Math.max(1, routine.getTargetIncrement() / 2);
        }

        Integer newTarget = currentTarget - decrement;

        // 최소값 보호
        return Math.max(newTarget, minimumTarget);
    }

    /**
     * 감소 조건 검증
     */
    private void validateReductionConditions(RoutineEntity routine, Long memberId) {
        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }

        if (!canDecreaseTarget(routine)) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_DECREASE_TARGET);
        }

        LocalDate endDate = LocalDate.now(clock).minusDays(1);
        LocalDate startDate = endDate.minusDays(routine.getGrowthCycleDays() - 1);

        List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
            .findByRoutineAndMemberAndDateRange(routine.getRoutineId(), memberId, startDate, endDate);

        if (!isReductionCycleCompleted(routine, recentRecords)) {
            throw new BaseException(ErrorCode.REDUCTION_CYCLE_NOT_COMPLETED);
        }
    }

    /**
     * 마지막 시도 날짜 조회
     */
    private LocalDate findLastAttemptDate(Long routineId, Long memberId, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(30); // 최근 30일 내에서 조회

        List<DailyRoutineEntity> recentRecords = dailyRoutineRepository
            .findByRoutineAndMemberAndDateRange(routineId, memberId, startDate, endDate);

        return recentRecords.stream()
            .map(DailyRoutineEntity::getPerformedDate)
            .max(LocalDate::compareTo)
            .orElse(null);
    }
}