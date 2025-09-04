package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.request.RoutinePerformanceRequest;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyRoutineService {

    private final DailyRoutineRepository dailyRoutineRepository;
    private final ConsecutiveDaysCalculator consecutiveDaysCalculator;

    public List<DailyRoutineEntity> saveRoutineRecords(Long memberId, LocalDate date,
                                                       List<RoutinePerformanceRequest> records) {

        dailyRoutineRepository.deleteByMemberIdAndPerformedDate(memberId, date);

        List<DailyRoutineEntity> entities = new ArrayList<>();

        for (RoutinePerformanceRequest record : records) {
            int consecutiveDays = consecutiveDaysCalculator.calculate(
                record.getRoutineId(), memberId, date, record.getPerformanceLevel());
            
            // 루틴의 성장 모드인 경우 성공/실패 카운트 업데이트
            updateCycleDays(record.getRoutine(), record.getPerformanceLevel());
            DailyRoutineEntity entity = DailyRoutineEntity.create(
                    record.getRoutine(),
                    record.getMember(),
                    record.getPerformanceLevel(),
                    date,
                    consecutiveDays
            );

            entities.add(entity);
        }

        return dailyRoutineRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<DailyRoutineEntity> getTodayRoutines(Long memberId, LocalDate date) {
        return dailyRoutineRepository.findByMemberIdAndPerformedDateWithRoutine(memberId, date);
    }
    
    /**
     * 성장 모드 루틴의 성공/실패 카운트 업데이트
     */
    private void updateCycleDays(RoutineEntity routine, PerformanceLevel performanceLevel) {
        // 성장 모드가 아니면 업데이트하지 않음
        if (!routine.isGrowthModeEnabled()) {
            return;
        }
        
        if (performanceLevel == PerformanceLevel.FULL_SUCCESS) {
            // 성공 시: currentCycleDays++, failureCycleDays=0 (리셋)
            routine.updateGrowthConfiguration(
                routine.getGrowthConfiguration()
                    .withIncrementedCycle()
                    .withResetFailureCycle()
            );
        } else {
            // 실패 시: failureCycleDays++, currentCycleDays=0 (리셋)
            routine.updateGrowthConfiguration(
                routine.getGrowthConfiguration()
                    .withIncrementedFailureCycle()
                    .withResetSuccessCycle()
            );
        }
    }
}