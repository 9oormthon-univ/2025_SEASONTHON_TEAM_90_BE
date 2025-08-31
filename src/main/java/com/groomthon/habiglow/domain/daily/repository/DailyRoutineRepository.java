package com.groomthon.habiglow.domain.daily.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;

public interface DailyRoutineRepository extends JpaRepository<DailyRoutineEntity, Long> {
    
    @Query("SELECT dr FROM DailyRoutineEntity dr " +
           "LEFT JOIN FETCH dr.routine " +
           "WHERE dr.member.id = :memberId AND dr.performedDate = :date")
    List<DailyRoutineEntity> findByMemberIdAndPerformedDateWithRoutine(@Param("memberId") Long memberId, 
                                                                       @Param("date") LocalDate date);
    
    Optional<DailyRoutineEntity> findByRoutineRoutineIdAndMemberIdAndPerformedDate(
        Long routineId, Long memberId, LocalDate date);
    
    boolean existsByMemberIdAndRoutineRoutineIdAndPerformedDate(Long memberId, Long routineId, LocalDate date);
    
    @Modifying
    @Query("DELETE FROM DailyRoutineEntity dr WHERE dr.member.id = :memberId AND dr.performedDate = :date")
    void deleteByMemberIdAndPerformedDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
}