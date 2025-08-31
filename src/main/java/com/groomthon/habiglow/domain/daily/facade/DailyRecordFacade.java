package com.groomthon.habiglow.domain.daily.facade;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.request.SaveDailyRecordRequest;
import com.groomthon.habiglow.domain.daily.dto.response.DailyRecordResponse;
import com.groomthon.habiglow.domain.daily.service.DailyRecordCommandService;
import com.groomthon.habiglow.domain.daily.service.DailyRecordQueryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyRecordFacade {
    
    private final DailyRecordCommandService commandService;
    private final DailyRecordQueryService queryService;
    
    @Transactional
    public DailyRecordResponse saveDailyRecord(Long memberId, LocalDate date, SaveDailyRecordRequest request) {
        return commandService.saveDailyRecord(memberId, date, request);
    }
    
    @Transactional(readOnly = true)
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        return queryService.getDailyRecord(memberId, date);
    }
}