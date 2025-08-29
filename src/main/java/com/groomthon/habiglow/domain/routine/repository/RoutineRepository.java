package com.groomthon.habiglow.domain.routine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

@Repository
public interface RoutineRepository extends JpaRepository<RoutineEntity, Long> {
    
    List<RoutineEntity> findByMember_Id(Long memberId);
    
    List<RoutineEntity> findByMember_IdAndDetails_Category(Long memberId, RoutineCategory category);
    
    Optional<RoutineEntity> findByRoutineIdAndMember_Id(Long routineId, Long memberId);
    
    Boolean existsByRoutineIdAndMember_Id(Long routineId, Long memberId);
}