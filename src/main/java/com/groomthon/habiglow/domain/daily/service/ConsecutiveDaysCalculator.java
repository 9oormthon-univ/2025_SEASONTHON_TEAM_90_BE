package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConsecutiveDaysCalculator {
    
    private final DailyRoutineRepository dailyRoutineRepository;
    
    @Cacheable(value = "consecutiveDays", key = "#routineId + '_' + #memberId + '_' + #currentDate")
    public int calculate(Long routineId, Long memberId, LocalDate currentDate, PerformanceLevel performance) {
        
        if (performance != PerformanceLevel.FULL_SUCCESS) {
            return 0;
        }
        
        LocalDate yesterday = currentDate.minusDays(1);
        Optional<DailyRoutineEntity> yesterdayRecord = dailyRoutineRepository
            .findByRoutineRoutineIdAndMemberIdAndPerformedDate(routineId, memberId, yesterday);
        
        if (yesterdayRecord.isEmpty() || !yesterdayRecord.get().isFullSuccess()) {
            return 1;
        }
        
        return yesterdayRecord.get().getConsecutiveDays() + 1;
    }
    
}