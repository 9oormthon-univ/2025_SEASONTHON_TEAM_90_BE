package com.groomthon.habiglow.domain.routine.facade;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.dto.request.CreateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.request.UpdateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.AdaptationAction;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.event.RoutineCreatedEvent;
import com.groomthon.habiglow.domain.routine.event.RoutineDeletedEvent;
import com.groomthon.habiglow.domain.routine.event.RoutineTargetChangedEvent;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.routine.service.RoutineValidationService;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RoutineManagementFacade의 세부 구현을 담당하는 Helper 클래스
 * 데이터 조회, 검증, 이벤트 발행, 로깅 등의 공통 로직을 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RoutineManagementHelper {

    private final RoutineRepository routineRepository;
    private final MemberRepository memberRepository;
    private final RoutineValidationService validationService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== DATA ACCESS METHODS ====================

    MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
    }

    RoutineEntity findRoutineByIdAndMemberId(Long routineId, Long memberId) {
        return routineRepository.findByRoutineIdAndMember_Id(routineId, memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
    }

    RoutineEntity createAndSaveRoutine(MemberEntity member, CreateRoutineRequest request) {
        RoutineEntity routine = RoutineEntity.createRoutine(
                member,
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getIsGrowthMode(),
                request.getTargetType(),
                request.getTargetValue(),
                request.getGrowthCycleDays(),
                request.getTargetIncrement()
        );
        
        return routineRepository.save(routine);
    }

    void deleteRoutine(RoutineEntity routine) {
        routineRepository.delete(routine);
    }

    // ==================== EVENT PUBLISHING METHODS ====================

    void publishRoutineCreatedEvent(RoutineEntity routine, Long memberId) {
        RoutineCreatedEvent event = RoutineCreatedEvent.of(
            routine.getRoutineId(), routine.getTitle(), memberId,
            routine.getCategory(), routine.getIsGrowthMode(),
            routine.getTargetType(), routine.getTargetValue()
        );
        eventPublisher.publishEvent(event);
    }

    void publishRoutineDeletedEvent(RoutineEntity routine, Long memberId) {
        RoutineDeletedEvent event = RoutineDeletedEvent.of(
            routine.getRoutineId(), routine.getTitle(), memberId,
            routine.getCategory(), routine.isGrowthModeEnabled(), 
            routine.getTargetValue()
        );
        eventPublisher.publishEvent(event);
    }

    void publishTargetChangedEvent(RoutineEntity routine, Long memberId, 
                                  Integer previousTarget, Integer newTarget, AdaptationAction action) {
        RoutineTargetChangedEvent event = RoutineTargetChangedEvent.of(
            routine.getRoutineId(), routine.getTitle(), memberId,
            previousTarget, newTarget, action
        );
        eventPublisher.publishEvent(event);
    }

    // ==================== VALIDATION METHODS ====================

    void validateAndCreateRoutine(CreateRoutineRequest request) {
        validationService.validateCreateRequest(
                request.getTitle(),
                request.getCategory(),
                request.getIsGrowthMode(),
                request.getTargetType(),
                request.getTargetValue(),
                request.getGrowthCycleDays(),
                request.getTargetIncrement()
        );
    }

    void validateAndUpdateRoutine(UpdateRoutineRequest request) {
        validationService.validateUpdateRequest(
                request.getDescription(),
                request.getCategory(),
                request.getIsGrowthMode(),
                request.getTargetType(),
                request.getTargetValue(),
                request.getGrowthCycleDays(),
                request.getTargetIncrement()
        );
    }

    // ==================== LOGGING METHODS ====================

    void handleGrowthModeLogging(RoutineEntity routine, Long memberId) {
        if (routine.isGrowthModeEnabled()) {
            logGrowthModeActivated(routine, memberId);
        }
    }

    void handleGrowthModeChange(RoutineEntity routine, boolean wasGrowthEnabled, Long memberId) {
        boolean isNowGrowthEnabled = routine.isGrowthModeEnabled();
        
        if (!wasGrowthEnabled && isNowGrowthEnabled) {
            log.info("Growth mode activated for routine: {} by member: {}", 
                    routine.getRoutineId(), memberId);
        } else if (wasGrowthEnabled && !isNowGrowthEnabled) {
            log.info("Growth mode deactivated for routine: {} by member: {}", 
                    routine.getRoutineId(), memberId);
        }
    }

    void handleDeletionLogging(RoutineEntity routine, Long memberId) {
        if (routine.isGrowthModeEnabled()) {
            log.info("Deleting growth-enabled routine: {} for member: {} - Lost target: {}", 
                    routine.getRoutineId(), memberId, routine.getTargetValue());
        } else {
            log.info("Deleting routine: {} for member: {}", routine.getRoutineId(), memberId);
        }
    }

    private void logGrowthModeActivated(RoutineEntity routine, Long memberId) {
        log.info("New routine with growth mode created: {} for member: {} - Target: {}, Increment: {}", 
                routine.getRoutineId(), memberId, routine.getTargetValue(), routine.getTargetIncrement());
    }
}