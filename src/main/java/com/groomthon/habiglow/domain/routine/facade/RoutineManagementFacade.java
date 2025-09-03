package com.groomthon.habiglow.domain.routine.facade;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.dto.request.CreateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.request.UpdateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.routine.service.RoutineValidationService;
import com.groomthon.habiglow.domain.routine.service.GrowthAnalysisService;
import com.groomthon.habiglow.domain.routine.service.ReductionAnalysisService;
import com.groomthon.habiglow.domain.routine.service.GrowthConfigurationService;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.RoutineAdaptationResultResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.AdaptationAction;
import com.groomthon.habiglow.domain.routine.event.RoutineTargetChangedEvent;
import com.groomthon.habiglow.domain.routine.event.RoutineCreatedEvent;
import com.groomthon.habiglow.domain.routine.event.RoutineDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineManagementFacade {
    
    private final RoutineRepository routineRepository;
    private final MemberRepository memberRepository;
    private final RoutineValidationService validationService;
    private final GrowthAnalysisService growthAnalysisService;
    private final ReductionAnalysisService reductionAnalysisService;
    private final GrowthConfigurationService growthConfigurationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RoutineResponse createRoutineWithFullValidation(Long memberId, CreateRoutineRequest request) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        validateAndCreateRoutine(request);
        
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
        
        RoutineEntity savedRoutine = routineRepository.save(routine);
        
        if (savedRoutine.isGrowthModeEnabled()) {
            logGrowthModeActivated(savedRoutine, memberId);
        }
        
        // 루틴 생성 이벤트 발행
        RoutineCreatedEvent event = RoutineCreatedEvent.of(
            savedRoutine.getRoutineId(), savedRoutine.getTitle(), memberId,
            savedRoutine.getCategory(), savedRoutine.getIsGrowthMode(),
            savedRoutine.getTargetType(), savedRoutine.getTargetValue()
        );
        eventPublisher.publishEvent(event);
        
        log.info("Created routine: {} for member: {}", savedRoutine.getRoutineId(), memberId);
        return RoutineResponse.from(savedRoutine);
    }

    @Transactional
    public RoutineResponse updateRoutineWithValidation(Long memberId, Long routineId, UpdateRoutineRequest request) {
        RoutineEntity routine = routineRepository.findByRoutineIdAndMember_Id(routineId, memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
        
        validateAndUpdateRoutine(request);
        
        boolean wasGrowthEnabled = routine.isGrowthModeEnabled();
        
        routine.updateRoutine(
                request.getDescription(),
                request.getCategory(),
                request.getIsGrowthMode(),
                request.getTargetType(),
                request.getTargetValue(),
                request.getGrowthCycleDays(),
                request.getTargetIncrement()
        );
        
        handleGrowthModeChange(routine, wasGrowthEnabled, memberId);
        
        log.info("Updated routine: {} for member: {}", routineId, memberId);
        return RoutineResponse.from(routine);
    }


    private void validateAndCreateRoutine(CreateRoutineRequest request) {
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

    private void validateAndUpdateRoutine(UpdateRoutineRequest request) {
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

    private void handleGrowthModeChange(RoutineEntity routine, boolean wasGrowthEnabled, Long memberId) {
        boolean isNowGrowthEnabled = routine.isGrowthModeEnabled();
        
        if (!wasGrowthEnabled && isNowGrowthEnabled) {
            log.info("Growth mode activated for routine: {} by member: {}", 
                    routine.getRoutineId(), memberId);
        } else if (wasGrowthEnabled && !isNowGrowthEnabled) {
            log.info("Growth mode deactivated for routine: {} by member: {}", 
                    routine.getRoutineId(), memberId);
        }
    }

    @Transactional
    public void deleteRoutine(Long memberId, Long routineId) {
        RoutineEntity routine = routineRepository.findByRoutineIdAndMember_Id(routineId, memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
        
        // 루틴 삭제 이벤트 발행 (삭제 전에 데이터 수집)
        RoutineDeletedEvent event = RoutineDeletedEvent.of(
            routine.getRoutineId(), routine.getTitle(), memberId,
            routine.getCategory(), routine.isGrowthModeEnabled(), 
            routine.getTargetValue()
        );
        eventPublisher.publishEvent(event);
        
        if (routine.isGrowthModeEnabled()) {
            log.info("Deleting growth-enabled routine: {} for member: {} - Lost target: {}", 
                    routineId, memberId, routine.getTargetValue());
        } else {
            log.info("Deleting routine: {} for member: {}", routineId, memberId);
        }
        
        routineRepository.delete(routine);
    }

    private void logGrowthModeActivated(RoutineEntity routine, Long memberId) {
        log.info("New routine with growth mode created: {} for member: {} - Target: {}, Increment: {}", 
                routine.getRoutineId(), memberId, routine.getTargetValue(), routine.getTargetIncrement());
    }

    /**
     * 루틴 적응 실행 - 통합된 Facade 메서드 (CQRS Command 부분)
     */
    @Transactional
    public RoutineAdaptationResultResponse executeRoutineAdaptation(Long memberId, Long routineId, AdaptationAction action) {
        RoutineEntity routine = routineRepository.findByRoutineIdAndMember_Id(routineId, memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
        
        return switch (action) {
            case INCREASE -> executeGrowthAdaptation(routine, memberId);
            case DECREASE -> executeReductionAdaptation(routine, memberId);
            case RESET -> executeGrowthReset(routine, memberId);
        };
    }
    
    private RoutineAdaptationResultResponse executeGrowthAdaptation(RoutineEntity routine, Long memberId) {
        growthAnalysisService.validateGrowthConditions(routine, memberId);
        
        Integer previousTarget = routine.getTargetValue();
        Integer newTarget = growthConfigurationService.executeTargetIncrease(routine);
        
        // 도메인 이벤트 발행
        RoutineTargetChangedEvent event = RoutineTargetChangedEvent.of(
            routine.getRoutineId(), routine.getTitle(), memberId,
            previousTarget, newTarget, AdaptationAction.INCREASE
        );
        eventPublisher.publishEvent(event);
        
        log.info("Growth adaptation executed for routine: {} from {} to {} by member: {}",
            routine.getRoutineId(), previousTarget, newTarget, memberId);
            
        return RoutineAdaptationResultResponse.success(
            routine.getRoutineId(), routine.getTitle(), 
            previousTarget, newTarget, AdaptationAction.INCREASE);
    }
    
    private RoutineAdaptationResultResponse executeReductionAdaptation(RoutineEntity routine, Long memberId) {
        reductionAnalysisService.validateReductionConditions(routine, memberId);
        
        Integer previousTarget = routine.getTargetValue();
        Integer newTarget = growthConfigurationService.executeTargetDecrease(routine);
        
        // 도메인 이벤트 발행
        RoutineTargetChangedEvent event = RoutineTargetChangedEvent.of(
            routine.getRoutineId(), routine.getTitle(), memberId,
            previousTarget, newTarget, AdaptationAction.DECREASE
        );
        eventPublisher.publishEvent(event);
        
        log.info("Reduction adaptation executed for routine: {} from {} to {} by member: {}",
            routine.getRoutineId(), previousTarget, newTarget, memberId);
            
        return RoutineAdaptationResultResponse.success(
            routine.getRoutineId(), routine.getTitle(), 
            previousTarget, newTarget, AdaptationAction.DECREASE);
    }
    
    private RoutineAdaptationResultResponse executeGrowthReset(RoutineEntity routine, Long memberId) {
        Integer previousCycleDays = routine.getCurrentCycleDays();
        growthConfigurationService.resetGrowthCycle(routine);
        
        log.info("Growth cycle reset for routine: {} by member: {}, previous cycle days: {}",
            routine.getRoutineId(), memberId, previousCycleDays);
            
        return RoutineAdaptationResultResponse.success(
            routine.getRoutineId(), routine.getTitle(), 
            previousCycleDays, 0, AdaptationAction.RESET);
    }

}