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
    
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, date);
        List<DailyRoutineEntity> routineRecords = dailyRoutineService.getTodayRoutines(memberId, date);
        List<RoutineEntity> allUserRoutines = routineService.getUserRoutines(memberId);
        
        return DailyRecordResponse.of(reflection.orElse(null), routineRecords, allUserRoutines);
    }
    
}