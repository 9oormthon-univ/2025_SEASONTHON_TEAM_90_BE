package com.groomthon.habiglow.domain.routine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

@Repository
public interface RoutineRepository extends JpaRepository<RoutineEntity, Long> {
    
    List<RoutineEntity> findByMember_Id(Long memberId);
    
    List<RoutineEntity> findByMember_IdAndDetails_Category(Long memberId, RoutineCategory category);
    
    Optional<RoutineEntity> findByRoutineIdAndMember_Id(Long routineId, Long memberId);
    
    @Query("SELECT r FROM RoutineEntity r WHERE r.member.id = :memberId AND r.growthConfiguration.isGrowthMode = true")
    List<RoutineEntity> findGrowthEnabledRoutinesByMemberId(@Param("memberId") Long memberId);
}