package com.groomthon.habiglow.domain.routine.service;

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
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoutineGrowthCheckService {
    
    private final RoutineRepository routineRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    
    public GrowthCheckResponse checkGrowthReadyRoutines(Long memberId) {
        // 1. 성장 모드 활성화된 루틴들 조회
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);
        
        if (growthRoutines.isEmpty()) {
            return GrowthCheckResponse.builder()
                    .growthReadyRoutines(Collections.emptyList())
                    .totalGrowthReadyCount(0)
                    .build();
        }
        
        // 2. 전날 날짜 계산
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // 3. 배치로 전날 성공 기록 조회 (성능 최적화)
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
        
        // 4. 각 루틴별 성장 주기 완료 확인
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
    
    private boolean isGrowthCycleCompleted(RoutineEntity routine, DailyRoutineEntity lastRecord) {
        // 1. 전날 성공 기록이 없으면 성장 불가
        if (lastRecord == null) {
            return false;
        }
        
        // 2. GrowthSettings의 isGrowthCycleCompleted 메서드 사용
        return routine.getGrowthSettings().isGrowthCycleCompleted();
    }
    
    // 성장 주기 완료 여부만 확인하는 유틸리티 메서드 (다른 서비스에서도 사용 가능)
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
}