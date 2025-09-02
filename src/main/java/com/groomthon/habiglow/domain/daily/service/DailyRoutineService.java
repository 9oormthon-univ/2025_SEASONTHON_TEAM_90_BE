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
            
            // 루틴의 성장 모드인 경우 currentCycleDays 업데이트
            updateCurrentCycleDays(record.getRoutine(), record.getPerformanceLevel());
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
     * 성장 모드 루틴의 currentCycleDays 업데이트
     */
    private void updateCurrentCycleDays(RoutineEntity routine, PerformanceLevel performanceLevel) {
        // 성장 모드가 아니면 업데이트하지 않음
        if (!routine.isGrowthModeEnabled()) {
            return;
        }
        
        // FULL_SUCCESS인 경우 증가, 아니면 리셋
        if (performanceLevel == PerformanceLevel.FULL_SUCCESS) {
            routine.getGrowthSettings().incrementCurrentCycleDays();
        } else {
            routine.getGrowthSettings().resetCurrentCycleDays();
        }
    }
}