package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.request.RoutinePerformanceRequest;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyRoutineService {

    private final DailyRoutineRepository dailyRoutineRepository;
    private final ConsecutiveDaysCalculator consecutiveDaysCalculator;
    private final MemberRepository memberRepository;

    public List<DailyRoutineEntity> saveRoutineRecords(Long memberId, LocalDate date,
                                                       List<RoutinePerformanceRequest> records) {

        List<DailyRoutineEntity> entities = new ArrayList<>();

        for (RoutinePerformanceRequest record : records) {
            // 기존 기록 확인 (upsert 방식)
            Optional<DailyRoutineEntity> existingRecord = dailyRoutineRepository
                .findByRoutine_RoutineIdAndMemberIdAndPerformedDate(
                    record.getRoutine().getRoutineId(), memberId, date);
            
            int consecutiveDays = consecutiveDaysCalculator.calculate(
                record.getRoutineId(), memberId, date, record.getPerformanceLevel());
            
            // 루틴의 성장 모드인 경우 성공/실패 카운트 업데이트
            updateCycleDays(record.getRoutine(), record.getPerformanceLevel());
            
            DailyRoutineEntity entity;
            if (existingRecord.isPresent()) {
                // 기존 기록 업데이트
                entity = existingRecord.get();
                entity.updatePerformance(record.getPerformanceLevel(), consecutiveDays);
            } else {
                // 새 기록 생성
                entity = DailyRoutineEntity.create(
                        record.getRoutine(),
                        record.getMember(),
                        record.getPerformanceLevel(),
                        date,
                        consecutiveDays
                );
            }

            entities.add(entity);
        }

        return dailyRoutineRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<DailyRoutineEntity> getTodayRoutines(Long memberId, LocalDate date) {
        return dailyRoutineRepository.findByMemberIdAndPerformedDateWithRoutine(memberId, date);
    }

    /**
     * 당일 조회용: 실제 기록과 가상 미수행 기록을 합쳐서 반환
     */
    @Transactional(readOnly = true)
    public List<DailyRoutineEntity> getTodayRecordWithVirtual(Long memberId, LocalDate date, 
                                                             List<RoutineEntity> allUserRoutines) {
        List<DailyRoutineEntity> actualRecords = dailyRoutineRepository
            .findByMemberIdAndPerformedDateWithRoutine(memberId, date);
        
        return mergeActualAndVirtualRecords(allUserRoutines, actualRecords, memberId, date);
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

    /**
     * 실제 기록과 가상 미수행 기록을 합치는 메서드
     */
    private List<DailyRoutineEntity> mergeActualAndVirtualRecords(
            List<RoutineEntity> allUserRoutines, 
            List<DailyRoutineEntity> actualRecords,
            Long memberId, 
            LocalDate date) {
        
        Set<Long> recordedRoutineIds = actualRecords.stream()
                .map(record -> record.getRoutine() != null ? record.getRoutine().getRoutineId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        List<DailyRoutineEntity> virtualRecords = allUserRoutines.stream()
                .filter(routine -> !recordedRoutineIds.contains(routine.getRoutineId()))
                .map(routine -> createVirtualNotPerformedRecord(routine, member, date))
                .collect(Collectors.toList());
        
        List<DailyRoutineEntity> completeRecords = new ArrayList<>(actualRecords);
        completeRecords.addAll(virtualRecords);
        
        return completeRecords;
    }

    /**
     * 가상 미수행 기록 생성
     */
    private DailyRoutineEntity createVirtualNotPerformedRecord(RoutineEntity routine, MemberEntity member, LocalDate date) {
        return DailyRoutineEntity.create(routine, member, PerformanceLevel.NOT_PERFORMED, date, 0);
    }
}