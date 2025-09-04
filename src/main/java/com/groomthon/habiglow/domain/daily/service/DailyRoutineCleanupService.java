package com.groomthon.habiglow.domain.daily.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 루틴 삭제 시 DailyRoutineEntity의 routine 참조를 null로 처리하는 서비스
 * 데이터 무결성을 보장하면서 히스토리 데이터를 유지함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DailyRoutineCleanupService {
    
    private final DailyRoutineRepository dailyRoutineRepository;
    
    /**
     * 루틴 삭제 시 관련된 모든 DailyRoutineEntity의 routine 참조를 null로 처리
     * 
     * @param routineId 삭제된 루틴 ID
     */
    public void nullifyRoutineReference(Long routineId) {
        int updatedCount = dailyRoutineRepository.nullifyRoutineReference(routineId);
        
        log.info("Nullified routine reference for {} daily routine records after routine deletion: {}", 
                updatedCount, routineId);
    }
    
    /**
     * 특정 회원의 루틴 삭제 시 관련된 DailyRoutineEntity의 routine 참조를 null로 처리
     * 
     * @param routineId 삭제된 루틴 ID  
     * @param memberId 회원 ID
     */
    public void nullifyRoutineReferenceForMember(Long routineId, Long memberId) {
        int updatedCount = dailyRoutineRepository.nullifyRoutineReferenceForMember(routineId, memberId);
        
        log.info("Nullified routine reference for {} daily routine records for member {} after routine deletion: {}", 
                updatedCount, memberId, routineId);
    }
}