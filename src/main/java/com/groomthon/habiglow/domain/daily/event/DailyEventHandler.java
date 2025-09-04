package com.groomthon.habiglow.domain.daily.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.service.DailyRoutineCleanupService;
import com.groomthon.habiglow.domain.routine.event.RoutineDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily 도메인 이벤트 처리
 * 루틴 삭제 시 DailyRoutineEntity의 데이터 정합성을 유지함
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class DailyEventHandler {
    
    private final DailyRoutineCleanupService cleanupService;
    
    /**
     * 루틴 삭제 이벤트 처리
     * DailyRoutineEntity의 routine 참조를 null로 처리하여 데이터를 보존
     */
    @EventListener
    public void handleRoutineDeleted(RoutineDeletedEvent event) {
        log.info("Processing routine deletion for daily records - RoutineId: {}, MemberId: {}", 
                event.getRoutineId(), event.getMemberId());
        
        cleanupService.nullifyRoutineReferenceForMember(event.getRoutineId(), event.getMemberId());
        
        log.info("Completed daily routine cleanup for deleted routine: {} by member: {}", 
                event.getRoutineId(), event.getMemberId());
    }
}