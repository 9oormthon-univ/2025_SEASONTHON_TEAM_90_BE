package com.groomthon.habiglow.domain.routine.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.request.CreateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.request.UpdateRoutineRequest;
import com.groomthon.habiglow.domain.routine.dto.response.GrowthAnalysisResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineListResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.domain.routine.facade.RoutineGrowthFacade;
import com.groomthon.habiglow.domain.routine.facade.RoutineManagementFacade;
import com.groomthon.habiglow.domain.routine.facade.RoutineQueryFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {
    
    private final RoutineManagementFacade managementFacade;
    private final RoutineGrowthFacade growthFacade;
    private final RoutineQueryFacade queryFacade;
    
    @Transactional
    public RoutineResponse createRoutine(Long memberId, CreateRoutineRequest request) {
        return managementFacade.createRoutineWithFullValidation(memberId, request);
    }
    
    public RoutineListResponse getMyRoutines(Long memberId) {
        return queryFacade.getMyRoutines(memberId);
    }
    
    public RoutineListResponse getMyRoutinesByCategory(Long memberId, RoutineCategory category) {
        return queryFacade.getMyRoutinesByCategory(memberId, category);
    }
    
    public RoutineResponse getRoutineById(Long memberId, Long routineId) {
        return queryFacade.getRoutineById(memberId, routineId);
    }
    
    public RoutineListResponse getGrowthEnabledRoutines(Long memberId) {
        return queryFacade.getGrowthEnabledRoutines(memberId);
    }
    
    @Transactional
    public RoutineResponse updateRoutine(Long memberId, Long routineId, UpdateRoutineRequest request) {
        return managementFacade.updateRoutineWithValidation(memberId, routineId, request);
    }
    
    @Transactional
    public void deleteRoutine(Long memberId, Long routineId) {
        managementFacade.deleteRoutine(memberId, routineId);
    }
    
    @Transactional
    public RoutineResponse increaseRoutineTarget(Long memberId, Long routineId) {
        return growthFacade.forceTargetIncrease(memberId, routineId);
    }
    
    @Transactional(readOnly = true)
    public GrowthAnalysisResponse analyzeRoutineGrowth(Long memberId, Long routineId) {
        return growthFacade.analyzeGrowthStatus(memberId, routineId);
    }
}