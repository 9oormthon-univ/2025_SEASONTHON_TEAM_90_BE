package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.request.SaveDailyRecordRequest;
import com.groomthon.habiglow.domain.daily.dto.response.DailyRecordResponse;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecordCommandService {
    
    private final DailyRecordDomainService domainService;
    private final DailyReflectionService reflectionService;
    
    public DailyRecordResponse saveDailyRecord(Long memberId, LocalDate date, SaveDailyRecordRequest request) {
        domainService.validateDateModifiable(date);
        
        DailyReflectionEntity reflection = saveReflectionIfPresent(memberId, date, request);
        List<DailyRoutineEntity> routineRecords = domainService.saveRoutineRecords(
            memberId, date, request.getRoutineRecords());
        
        return DailyRecordResponse.of(reflection, routineRecords);
    }
    
    
    private DailyReflectionEntity saveReflectionIfPresent(Long memberId, LocalDate date, 
                                                         SaveDailyRecordRequest request) {
        if (request.getReflection() == null) {
            return null;
        }
        
        return reflectionService.saveReflection(
            memberId, date, 
            request.getReflection().getContent(),
            request.getReflection().getEmotion()
        );
    }
}