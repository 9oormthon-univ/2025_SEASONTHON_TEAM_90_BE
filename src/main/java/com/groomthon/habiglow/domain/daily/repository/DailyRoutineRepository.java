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

    @Query("SELECT dr FROM DailyRoutineEntity dr WHERE dr.routine.routineId = :routineId " +
        "AND dr.member.id = :memberId " +
        "AND dr.performedDate BETWEEN :startDate AND :endDate " +
        "ORDER BY dr.performedDate DESC")
    List<DailyRoutineEntity> findByRoutineAndMemberAndDateRange(
        @Param("routineId") Long routineId,
        @Param("memberId") Long memberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

// 주간 범위 조회 (member.id + performedDate BETWEEN)
    @Query("SELECT dr FROM DailyRoutineEntity dr " +
            "WHERE dr.member.id = :memberId " +
            "AND dr.performedDate BETWEEN :start AND :end " +
            "ORDER BY dr.performedDate ASC")
    List<DailyRoutineEntity> findByMemberIdAndPerformedDateBetween(
            @Param("memberId") Long memberId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * 루틴 삭제 시 관련된 모든 DailyRoutineEntity의 routine 참조를 null로 처리
     */
    @Modifying
    @Query("UPDATE DailyRoutineEntity dr SET dr.routine = null WHERE dr.routine.routineId = :routineId")
    int nullifyRoutineReference(@Param("routineId") Long routineId);

    /**
     * 특정 회원의 루틴 삭제 시 관련된 DailyRoutineEntity의 routine 참조를 null로 처리
     */
    @Modifying
    @Query("UPDATE DailyRoutineEntity dr SET dr.routine = null " +
           "WHERE dr.routine.routineId = :routineId AND dr.member.id = :memberId")
    int nullifyRoutineReferenceForMember(@Param("routineId") Long routineId, @Param("memberId") Long memberId);

    /**
     * 특정 회원의 특정 날짜에 데일리 루틴 기록이 존재하는지 확인
     */
    boolean existsByMemberIdAndPerformedDate(Long memberId, LocalDate performedDate);

    /**
     * 특정 회원의 월별 통계를 위한 일별 루틴 성공률 조회
     */
    @Query("SELECT dr.performedDate, " +
           "COUNT(*) as totalRoutines, " +
           "COUNT(CASE WHEN dr.performanceLevel = 'FULL_SUCCESS' THEN 1 END) as successfulRoutines " +
           "FROM DailyRoutineEntity dr " +
           "WHERE dr.member.id = :memberId " +
           "AND EXTRACT(YEAR FROM dr.performedDate) = :year " +
           "AND EXTRACT(MONTH FROM dr.performedDate) = :month " +
           "GROUP BY dr.performedDate " +
           "ORDER BY dr.performedDate")
    List<Object[]> findMonthlyStatsByMemberAndYearMonth(@Param("memberId") Long memberId, 
                                                        @Param("year") int year, 
                                                        @Param("month") int month);

}