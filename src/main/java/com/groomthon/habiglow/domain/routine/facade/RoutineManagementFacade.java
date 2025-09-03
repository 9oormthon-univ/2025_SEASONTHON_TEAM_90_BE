package com.groomthon.habiglow.domain.routine.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.dto.request.CreateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.request.UpdateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.AdaptationAction;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.RoutineAdaptationResultResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.routine.service.GrowthAnalysisService;
import com.groomthon.habiglow.domain.routine.service.GrowthConfigurationService;
import com.groomthon.habiglow.domain.routine.service.ReductionAnalysisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineManagementFacade {
    
    private final RoutineRepository routineRepository;
    private final RoutineManagementHelper helper;
    private final GrowthAnalysisService growthAnalysisService;
    private final ReductionAnalysisService reductionAnalysisService;
    private final GrowthConfigurationService growthConfigurationService;

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Transactional
    public RoutineResponse createRoutineWithFullValidation(Long memberId, CreateRoutineRequest request) {
        MemberEntity member = helper.findMemberById(memberId);
        
        helper.validateAndCreateRoutine(request);
        
        RoutineEntity savedRoutine = helper.createAndSaveRoutine(member, request);
        
        helper.handleGrowthModeLogging(savedRoutine, memberId);
        
        helper.publishRoutineCreatedEvent(savedRoutine, memberId);
        
        log.info("Created routine: {} for member: {}", savedRoutine.getRoutineId(), memberId);
        return RoutineResponse.from(savedRoutine);
    }

    @Transactional
    public RoutineResponse updateRoutineWithValidation(Long memberId, Long routineId, UpdateRoutineRequest request) {
        RoutineEntity routine = helper.findRoutineByIdAndMemberId(routineId, memberId);
        
        helper.validateAndUpdateRoutine(request);
        
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
        
        helper.handleGrowthModeChange(routine, wasGrowthEnabled, memberId);
        
        log.info("Updated routine: {} for member: {}", routineId, memberId);
        return RoutineResponse.from(routine);
    }

    @Transactional
    public void deleteRoutine(Long memberId, Long routineId) {
        RoutineEntity routine = helper.findRoutineByIdAndMemberId(routineId, memberId);
        
        helper.publishRoutineDeletedEvent(routine, memberId);
        
        helper.handleDeletionLogging(routine, memberId);
        
        helper.deleteRoutine(routine);
    }

    // ==================== OPTIMIZATION MANAGEMENT ====================

    /**
     * 루틴 적응 실행 - 통합된 Facade 메서드 (CQRS Command 부분)
     */
    @Transactional
    public RoutineAdaptationResultResponse executeRoutineAdaptation(Long memberId, Long routineId, AdaptationAction action) {
        RoutineEntity routine = helper.findRoutineByIdAndMemberId(routineId, memberId);
        
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
        
        helper.publishTargetChangedEvent(routine, memberId, previousTarget, newTarget, AdaptationAction.INCREASE);
        
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
        
        helper.publishTargetChangedEvent(routine, memberId, previousTarget, newTarget, AdaptationAction.DECREASE);
        
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