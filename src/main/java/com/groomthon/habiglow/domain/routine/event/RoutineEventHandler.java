package com.groomthon.habiglow.domain.routine.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 루틴 관련 도메인 이벤트 처리
 * 필수 부수 효과만 비동기로 처리
 */
@Slf4j
@Component
public class RoutineEventHandler {

    /**
     * 루틴 목표 변경 이벤트 처리
     * - 로깅 및 필요 시 알림 발송
     */
    @Async
    @EventListener
    public void handleRoutineTargetChanged(RoutineTargetChangedEvent event) {
        log.info("Routine target changed - ID: {}, Title: {}, Member: {}, Action: {}, Change: {} -> {}",
                event.getRoutineId(), event.getRoutineTitle(), event.getMemberId(),
                event.getAction(), event.getPreviousTarget(), event.getNewTarget());
        
        // 필요 시 여기서 알림 서비스 호출
        // notificationService.sendTargetChangeNotification(event);
        
        // 필요 시 여기서 통계 업데이트
        // statisticsService.updateRoutineAdaptationStats(event);
    }
}