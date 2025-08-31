package com.groomthon.habiglow.domain.routine.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthAnalysisResponse;
import com.groomthon.habiglow.domain.routine.dto.response.IncreaseTargetResponse;
import com.groomthon.habiglow.domain.routine.dto.response.ResetGrowthCycleResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.helper.RoutineHelper;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {
    
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineHelper routineHelper;

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
    
    @Transactional
    public IncreaseTargetResponse increaseRoutineTarget(Long routineId, Long memberId) {
        // 1. 루틴 조회 및 기본 검증
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
        // 2. 성장 조건 검증 (모든 검증을 한 곳에서 처리)
        validateGrowthConditions(routine, memberId);
        
        // 3. 목표치 증가 실행
        Integer previousTarget = routine.getTargetValue();
        routine.increaseTarget(); // 기존 Entity 메서드 활용
        
        // 4. 로그 기록
        log.info("Target increased for routine: {} from {} to {} by member: {}", 
                routineId, previousTarget, routine.getTargetValue(), memberId);
        
        // 5. 응답 생성
        return IncreaseTargetResponse.from(routine, previousTarget);
    }
    
    @Transactional
    public ResetGrowthCycleResponse resetGrowthCycle(Long routineId, Long memberId) {
        // 1. 루틴 조회 및 기본 검증
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
        // 2. 성장 모드 확인
        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }
        
        // 3. 성장 주기 완료 상태 확인 (완료되지 않았으면 리셋할 필요 없음)
        if (!routine.getGrowthSettings().isGrowthCycleCompleted()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }
        
        // 4. 현재 주기일 저장 (응답용)
        Integer previousCycleDays = routine.getCurrentCycleDays();
        
        // 5. 성장 주기 리셋
        routine.getGrowthSettings().resetCurrentCycleDays();
        
        // 6. 로그 기록
        log.info("Growth cycle reset for routine: {} by member: {}, previous cycle days: {}", 
                routineId, memberId, previousCycleDays);
        
        // 7. 응답 생성
        return ResetGrowthCycleResponse.from(routine, previousCycleDays);
    }
    
    private void validateGrowthConditions(RoutineEntity routine, Long memberId) {
        // 1. 성장 모드 확인
        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }
        
        // 2. 목표 증가 가능 여부 확인
        if (!routine.canIncreaseTarget()) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_INCREASE_TARGET);
        }
        
        // 3. 성장 주기 완료 확인 (GrowthSettings의 메서드 사용)
        if (!routine.getGrowthSettings().isGrowthCycleCompleted()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }
        
        // 4. 전날 성공 기록 확인 (추가 안전장치)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
                .findSuccessRecordByRoutineAndMemberAndDate(routine.getRoutineId(), memberId, yesterday, PerformanceLevel.FULL_SUCCESS);
        
        if (lastRecord.isEmpty()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }
    }
}