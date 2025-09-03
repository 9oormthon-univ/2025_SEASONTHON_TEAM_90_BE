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

    /**
     * 루틴 생성 이벤트 처리
     * - 생성 로깅 및 초기 통계 설정
     */
    @Async
    @EventListener
    public void handleRoutineCreated(RoutineCreatedEvent event) {
        log.info("Routine created - ID: {}, Title: {}, Member: {}, Category: {}, GrowthMode: {}, Target: {}{}",
                event.getRoutineId(), event.getRoutineTitle(), event.getMemberId(),
                event.getCategory(), event.getIsGrowthMode(), event.getTargetValue(),
                event.getTargetType() != null ? event.getTargetType().getUnit() : "");
        
        // 필요 시 여기서 사용자 온보딩 진행상황 업데이트
        // onboardingService.updateRoutineCreationProgress(event.getMemberId());
        
        // 필요 시 여기서 카테고리별 통계 초기화
        // statisticsService.initializeRoutineStats(event);
    }

    /**
     * 루틴 삭제 이벤트 처리
     * - 삭제 로깅 및 관련 데이터 정리
     */
    @Async
    @EventListener
    public void handleRoutineDeleted(RoutineDeletedEvent event) {
        log.info("Routine deleted - ID: {}, Title: {}, Member: {}, Category: {}, WasGrowthEnabled: {}, FinalTarget: {}",
                event.getRoutineId(), event.getRoutineTitle(), event.getMemberId(),
                event.getCategory(), event.getWasGrowthEnabled(), event.getFinalTargetValue());
        
        // 필요 시 여기서 관련 통계 데이터 아카이빙
        // statisticsService.archiveRoutineStats(event);
        
        // 필요 시 여기서 관련 알림 및 스케줄 정리
        // scheduleService.cleanupRoutineSchedules(event.getRoutineId());
    }

    /**
     * 성장 주기 완료 이벤트 처리
     * - 주기 완료 알림 및 성취 기록
     */
    @Async
    @EventListener
    public void handleGrowthCycleCompleted(GrowthCycleCompletedEvent event) {
        log.info("Growth cycle completed - Routine ID: {}, Title: {}, Member: {}, Target: {}, Cycle: {}일, Completed: {}회",
                event.getRoutineId(), event.getRoutineTitle(), event.getMemberId(),
                event.getCurrentTarget(), event.getCycleDays(), event.getCompletedCycles());
        
        // 필요 시 여기서 성취 알림 발송
        // notificationService.sendGrowthCycleCompletedNotification(event);
        
        // 필요 시 여기서 성취 배지 지급
        // achievementService.checkAndAwardGrowthMilestones(event);
    }
}