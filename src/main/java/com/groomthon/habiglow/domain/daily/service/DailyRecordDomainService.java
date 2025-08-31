package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.error.InvalidRoutineError;
import com.groomthon.habiglow.domain.daily.dto.request.RoutinePerformanceRequest;
import com.groomthon.habiglow.domain.daily.dto.request.SaveDailyRecordRequest.RoutineRecordRequest;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.exception.DailyRecordValidationException;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.service.RoutineService;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecordDomainService {
    
    private final RoutineService routineService;
    private final MemberRepository memberRepository;
    private final DailyRoutineService dailyRoutineService;
    
    public List<DailyRoutineEntity> saveRoutineRecords(Long memberId, LocalDate date, 
                                                      List<RoutineRecordRequest> routineRecords) {
        if (routineRecords == null || routineRecords.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RoutinePerformanceRequest> enrichedRecords = enrichRoutineRecords(routineRecords, memberId);
        return dailyRoutineService.saveRoutineRecords(memberId, date, enrichedRecords);
    }
    
    public void validateRoutineOwnership(List<Long> routineIds, Long memberId) {
        List<RoutineEntity> routines = routineService.findAllByIds(routineIds);
        
        List<InvalidRoutineError> invalidRoutines = new ArrayList<>();
        
        for (Long routineId : routineIds) {
            RoutineEntity routine = routines.stream()
                .filter(r -> r.getRoutineId().equals(routineId))
                .findFirst()
                .orElse(null);
                
            if (routine == null) {
                invalidRoutines.add(InvalidRoutineError.notFound(routineId));
            } else if (!routine.isOwnedBy(memberId)) {
                invalidRoutines.add(InvalidRoutineError.accessDenied(routineId));
            }
        }
        
        if (!invalidRoutines.isEmpty()) {
            throw new DailyRecordValidationException(invalidRoutines);
        }
    }
    
    public void validateDateModifiable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new BaseException(ErrorCode.DAILY_RECORD_FUTURE_DATE_NOT_ALLOWED);
        }
    }
    
    private List<RoutinePerformanceRequest> enrichRoutineRecords(
            List<RoutineRecordRequest> records, Long memberId) {
        
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        Set<Long> routineIds = records.stream()
            .map(RoutineRecordRequest::getRoutineId)
            .collect(Collectors.toSet());
        
        validateRoutineOwnership(new ArrayList<>(routineIds), memberId);
        
        Map<Long, RoutineEntity> routineMap = routineService.findAllByIds(routineIds)
            .stream()
            .collect(Collectors.toMap(RoutineEntity::getRoutineId, Function.identity()));
        
        return records.stream()
            .map(record -> {
                RoutineEntity routine = routineMap.get(record.getRoutineId());
                return RoutinePerformanceRequest.of(routine, member, record.getPerformanceLevel());
            })
            .collect(Collectors.toList());
    }
}