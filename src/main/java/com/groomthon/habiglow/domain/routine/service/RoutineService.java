package com.groomthon.habiglow.domain.routine.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {
    private final RoutineRepository routineRepository;
    
    public List<RoutineEntity> findAllByIds(Collection<Long> routineIds) {
        return routineRepository.findAllById(routineIds);
    }
    
    public List<RoutineEntity> getUserRoutines(Long memberId) {
        return routineRepository.findByMember_Id(memberId);
    }
}