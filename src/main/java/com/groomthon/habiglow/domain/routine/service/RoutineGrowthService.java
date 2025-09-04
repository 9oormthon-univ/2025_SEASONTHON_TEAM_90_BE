package com.groomthon.habiglow.domain.routine.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.response.adaptation.AdaptiveRoutineCheckResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.GrowthReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.ReductionReadyRoutineResponse;
import com.groomthon.habiglow.domain.routine.dto.response.adaptation.RoutineAdaptationCheckResponse;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {

    private final RoutineRepository routineRepository;
    private final GrowthAnalysisService growthAnalysisService;
    private final ReductionAnalysisService reductionAnalysisService;
    private final GrowthConfigurationService growthConfigurationService;

    @Transactional(readOnly = true)
    public AdaptiveRoutineCheckResponse checkAdaptiveRoutines(Long memberId) {
        RoutineAdaptationCheckResponse<GrowthReadyRoutineResponse> growthResponse = growthAnalysisService.analyzeGrowthReadyRoutines(memberId);
        RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> reductionResponse = reductionAnalysisService.analyzeReductionReadyRoutines(memberId);

        List<GrowthReadyRoutineResponse> growthReadyRoutines = growthResponse.getCandidates();
        
        Set<Long> growthReadyIds = growthReadyRoutines.stream()
            .map(GrowthReadyRoutineResponse::getRoutineId)
            .collect(Collectors.toSet());

        List<ReductionReadyRoutineResponse> filteredReductionRoutines = 
            reductionResponse.getCandidates().stream()
            .filter(routine -> !growthReadyIds.contains(routine.getRoutineId()))
            .toList();

        log.info("Adaptive check completed for member: {}, found {} growth-ready and {} reduction-ready routines",
            memberId, growthReadyRoutines.size(), filteredReductionRoutines.size());

        return AdaptiveRoutineCheckResponse.of(growthReadyRoutines, filteredReductionRoutines);
    }

}