package com.groomthon.habiglow.domain.routine.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 루틴 도메인의 공통 데이터 접근 로직을 담당하는 서비스
 * Analysis 서비스들 간의 중복되는 데이터 조회 로직을 통합
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineDataAccessService {

    private final RoutineRepository routineRepository;
    private final DailyRoutineRepository dailyRoutineRepository;

    /**
     * 성장 모드가 활성화된 루틴들을 조회
     */
    public List<RoutineEntity> findGrowthEnabledRoutines(Long memberId) {
        log.debug("Finding growth-enabled routines for member: {}", memberId);
        return routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);
    }

    /**
     * 여러 루틴의 특정 날짜 성공 기록들을 Map으로 반환
     */
    public Map<Long, DailyRoutineEntity> getSuccessRecords(List<Long> routineIds, 
                                                          Long memberId, LocalDate date) {
        log.debug("Getting success records for {} routines on {} for member: {}", 
                 routineIds.size(), date, memberId);
        
        List<DailyRoutineEntity> records = dailyRoutineRepository
                .findSuccessRecordsByRoutinesAndMemberAndDate(
                    routineIds, memberId, date, PerformanceLevel.FULL_SUCCESS);
        
        return records.stream()
                .collect(Collectors.toMap(
                    record -> record.getRoutine().getRoutineId(),
                    record -> record,
                    (existing, replacement) -> existing // 중복 시 첫 번째 유지
                ));
    }

    /**
     * 단일 루틴의 특정 날짜 성공 기록 조회
     */
    public DailyRoutineEntity getSuccessRecord(Long routineId, Long memberId, LocalDate date) {
        log.debug("Getting success record for routine: {} member: {} on {}", 
                 routineId, memberId, date);
        return dailyRoutineRepository.findSuccessRecordByRoutineAndMemberAndDate(
            routineId, memberId, date, PerformanceLevel.FULL_SUCCESS)
            .orElse(null);
    }

    /**
     * 어제 성공한 루틴 기록들을 Map으로 반환 (편의 메서드)
     */
    public Map<Long, DailyRoutineEntity> getYesterdaySuccessRecords(List<Long> routineIds, 
                                                                  Long memberId, Clock clock) {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        return getSuccessRecords(routineIds, memberId, yesterday);
    }
}