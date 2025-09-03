package com.groomthon.habiglow.domain.routine.facade;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.dto.response.RoutineListResponse;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 루틴 조회 관련 복잡한 로직을 처리하는 Facade
 * 단순 조회부터 복잡한 필터링까지 통합 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineQueryFacade {
    
    private final RoutineRepository routineRepository;

    /**
     * 사용자의 모든 루틴 조회
     */
    public RoutineListResponse getMyRoutines(Long memberId) {
        List<RoutineResponse> routines = routineRepository.findByMember_Id(memberId)
                .stream()
                .map(RoutineResponse::from)
                .toList();
        
        log.debug("Retrieved {} routines for member: {}", routines.size(), memberId);
        return RoutineListResponse.of(routines);
    }

    /**
     * 카테고리별 루틴 조회
     */
    public RoutineListResponse getMyRoutinesByCategory(Long memberId, RoutineCategory category) {
        List<RoutineResponse> routines = routineRepository.findByMember_IdAndDetails_Category(memberId, category)
                .stream()
                .map(RoutineResponse::from)
                .toList();
        
        log.debug("Retrieved {} routines in category {} for member: {}", 
                routines.size(), category, memberId);
        return RoutineListResponse.of(routines);
    }

    /**
     * 특정 루틴 상세 조회
     */
    public RoutineResponse getRoutineById(Long memberId, Long routineId) {
        RoutineEntity routine = routineRepository.findByRoutineIdAndMember_Id(routineId, memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
        
        log.debug("Retrieved routine: {} for member: {}", routineId, memberId);
        return RoutineResponse.from(routine);
    }

}