package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.response.DailyRecordResponse;
import com.groomthon.habiglow.domain.daily.dto.response.MonthlyStatsResponse;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.service.RoutineService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyRecordQueryService {
    
    private final DailyReflectionService reflectionService;
    private final DailyRoutineService dailyRoutineService;
    private final RoutineService routineService;
    private final DailyRoutineRepository dailyRoutineRepository;
    
    /**
     * 과거 날짜 조회: 실제 저장된 기록만 반환
     */
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, date);
        List<DailyRoutineEntity> routineRecords = dailyRoutineService.getTodayRoutines(memberId, date);
        
        return DailyRecordResponse.of(reflection.orElse(null), routineRecords);
    }

    /**
     * 당일 조회: 실제 기록 + 가상 미수행 기록 합쳐서 반환
     */
    public DailyRecordResponse getTodayRecord(Long memberId, LocalDate date) {
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, date);
        List<RoutineEntity> allUserRoutines = routineService.getUserRoutines(memberId);
        List<DailyRoutineEntity> routineRecords = dailyRoutineService.getTodayRecordWithVirtual(memberId, date, allUserRoutines);
        
        return DailyRecordResponse.of(reflection.orElse(null), routineRecords, allUserRoutines);
    }
    
    /**
     * 특정 월의 일별 루틴 성공률 통계 조회
     */
    public MonthlyStatsResponse getMonthlyStats(Long memberId, int year, int month) {
        List<Object[]> rawStats = dailyRoutineRepository.findMonthlyStatsByMemberAndYearMonth(memberId, year, month);
        
        List<MonthlyStatsResponse.DailyStat> dailyStats = new ArrayList<>();
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        
        for (int day = 1; day <= daysInMonth; day++) {
            boolean hasData = false;
            for (Object[] raw : rawStats) {
                LocalDate date = (LocalDate) raw[0];
                if (date.getDayOfMonth() == day) {
                    Long totalRoutines = (Long) raw[1];
                    Long successfulRoutines = (Long) raw[2];
                    double successRate = totalRoutines > 0 ? (double) successfulRoutines / totalRoutines * 100 : 0.0;
                    
                    dailyStats.add(MonthlyStatsResponse.DailyStat.builder()
                        .day(day)
                        .totalRoutines(totalRoutines.intValue())
                        .successfulRoutines(successfulRoutines.intValue())
                        .successRate(Math.round(successRate * 100.0) / 100.0)
                        .build());
                    hasData = true;
                    break;
                }
            }
            
            if (!hasData) {
                dailyStats.add(MonthlyStatsResponse.DailyStat.builder()
                    .day(day)
                    .totalRoutines(0)
                    .successfulRoutines(0)
                    .successRate(0.0)
                    .build());
            }
        }
        
        return MonthlyStatsResponse.builder()
            .year(year)
            .month(month)
            .dailyStats(dailyStats)
            .build();
    }
}