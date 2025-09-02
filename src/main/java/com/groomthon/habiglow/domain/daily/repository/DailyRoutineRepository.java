package com.groomthon.habiglow.domain.daily.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;

public interface DailyRoutineRepository extends JpaRepository<DailyRoutineEntity, Long> {

    @Query("SELECT dr FROM DailyRoutineEntity dr " +
            "LEFT JOIN FETCH dr.routine " +
            "WHERE dr.member.id = :memberId AND dr.performedDate = :date")
    List<DailyRoutineEntity> findByMemberIdAndPerformedDateWithRoutine(@Param("memberId") Long memberId,
                                                                       @Param("date") LocalDate date);

    Optional<DailyRoutineEntity> findByRoutine_RoutineIdAndMemberIdAndPerformedDate(
            Long routineId, Long memberId, LocalDate date);

    @Modifying
    @Query("DELETE FROM DailyRoutineEntity dr WHERE dr.member.id = :memberId AND dr.performedDate = :date")
    void deleteByMemberIdAndPerformedDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
    
    @Query("SELECT dr FROM DailyRoutineEntity dr WHERE dr.routine.routineId = :routineId " +
           "AND dr.member.id = :memberId AND dr.performedDate = :date " +
           "AND dr.performanceLevel = :level")
    Optional<DailyRoutineEntity> findSuccessRecordByRoutineAndMemberAndDate(
        @Param("routineId") Long routineId,
        @Param("memberId") Long memberId, 
        @Param("date") LocalDate date,
        @Param("level") PerformanceLevel level);

    @Query("SELECT dr FROM DailyRoutineEntity dr WHERE dr.routine.routineId IN :routineIds " +
           "AND dr.member.id = :memberId AND dr.performedDate = :date " +
           "AND dr.performanceLevel = :level")
    List<DailyRoutineEntity> findSuccessRecordsByRoutinesAndMemberAndDate(
        @Param("routineIds") List<Long> routineIds,
        @Param("memberId") Long memberId,
        @Param("date") LocalDate date,
        @Param("level") PerformanceLevel level);

// 주간 범위 조회 (member.id + performedDate BETWEEN)
    @Query("SELECT dr FROM DailyRoutineEntity dr " +
            "WHERE dr.member.id = :memberId " +
            "AND dr.performedDate BETWEEN :start AND :end " +
            "ORDER BY dr.performedDate ASC")
    List<DailyRoutineEntity> findByMemberIdAndPerformedDateBetween(
            @Param("memberId") Long memberId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);


}