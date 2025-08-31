package com.groomthon.habiglow.domain.daily.facade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.groomthon.habiglow.domain.daily.dto.request.RoutinePerformanceRequest;
import com.groomthon.habiglow.domain.daily.dto.request.SaveDailyRecordRequest;
import com.groomthon.habiglow.domain.daily.dto.request.SaveDailyRecordRequest.RoutineRecordRequest;
import com.groomthon.habiglow.domain.daily.dto.response.DailyRecordResponse;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.service.DailyReflectionService;
import com.groomthon.habiglow.domain.daily.service.DailyRoutineService;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.service.RoutineService;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class DailyRecordFacade {
    
    private final DailyRoutineService dailyRoutineService;
    private final DailyReflectionService reflectionService;
    private final RoutineService routineService;
    private final MemberRepository memberRepository;
    
    public DailyRecordResponse saveDailyRecord(Long memberId, LocalDate date, SaveDailyRecordRequest request) {
        
        validateDateModifiable(date);
        
        DailyReflectionEntity reflection = null;
        if (request.getReflection() != null) {
            reflection = reflectionService.saveReflection(
                memberId, date, 
                request.getReflection().getContent(),
                request.getReflection().getEmotion()
            );
        }
        
        List<DailyRoutineEntity> routineRecords = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getRoutineRecords())) {
            List<RoutinePerformanceRequest> enrichedRecords = enrichRoutineRecords(
                request.getRoutineRecords(), memberId);
            
            routineRecords = dailyRoutineService.saveRoutineRecords(memberId, date, enrichedRecords);
        }
        
        return DailyRecordResponse.of(reflection, routineRecords);
    }
    
    @Transactional(readOnly = true)
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, date);
        
        List<DailyRoutineEntity> routineRecords = dailyRoutineService.getTodayRoutines(memberId, date);
        
        List<RoutineEntity> allUserRoutines = routineService.getUserRoutines(memberId);
        
        return DailyRecordResponse.of(reflection.orElse(null), routineRecords, allUserRoutines);
    }
    
    private void validateDateModifiable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (!date.equals(today)) {
            throw new IllegalArgumentException("당일 데이터만 수정 가능합니다.");
        }
    }
    
    private List<RoutinePerformanceRequest> enrichRoutineRecords(
            List<RoutineRecordRequest> records, Long memberId) {
        
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        Set<Long> routineIds = records.stream()
            .map(RoutineRecordRequest::getRoutineId)
            .collect(Collectors.toSet());
        
        Map<Long, RoutineEntity> routineMap = routineService.findAllByIds(routineIds)
            .stream()
            .collect(Collectors.toMap(RoutineEntity::getRoutineId, Function.identity()));
        
        return records.stream()
            .map(record -> {
                RoutineEntity routine = routineMap.get(record.getRoutineId());
                if (routine == null) {
                    throw new BaseException(ErrorCode.ROUTINE_NOT_FOUND);
                }
                return RoutinePerformanceRequest.of(routine, member, record.getPerformanceLevel());
            })
            .collect(Collectors.toList());
    }
}