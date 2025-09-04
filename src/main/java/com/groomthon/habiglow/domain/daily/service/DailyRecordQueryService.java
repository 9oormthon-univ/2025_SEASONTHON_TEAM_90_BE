package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.response.DailyRecordResponse;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
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
    
}