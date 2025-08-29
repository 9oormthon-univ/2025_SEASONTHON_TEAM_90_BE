package com.groomthon.habiglow.domain.routine.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.dto.request.CreateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.request.UpdateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.helper.RoutineHelper;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.routine.service.RoutineValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineManagementFacade {
    
    private final RoutineRepository routineRepository;
    private final RoutineHelper routineHelper;
    private final RoutineValidationService validationService;

    @Transactional
    public RoutineResponse createRoutineWithFullValidation(Long memberId, CreateRoutineRequest request) {
        MemberEntity member = routineHelper.findMemberById(memberId);
        
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
        
        log.info("Created routine: {} for member: {}", savedRoutine.getRoutineId(), memberId);
        return RoutineResponse.from(savedRoutine);
    }

    @Transactional
    public RoutineResponse updateRoutineWithValidation(Long memberId, Long routineId, UpdateRoutineRequest request) {
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
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
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
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

}