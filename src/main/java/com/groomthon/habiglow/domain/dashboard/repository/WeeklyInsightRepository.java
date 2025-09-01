package com.groomthon.habiglow.domain.dashboard.repository;

import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity;
import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity.AnalysisType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyInsightRepository extends JpaRepository<WeeklyInsightEntity, Long> {

    /**
     * 특정 사용자의 특정 주차 분석 결과 조회
     */
    Optional<WeeklyInsightEntity> findByMemberIdAndWeekStartDate(Long memberId, LocalDate weekStartDate);

    /**
     * 특정 사용자의 특정 주차, 특정 타입 분석 결과 조회
     */
    Optional<WeeklyInsightEntity> findByMemberIdAndWeekStartDateAndAnalysisType(
            Long memberId, LocalDate weekStartDate, AnalysisType analysisType);

    /**
     * 특정 사용자의 모든 분석 결과 조회 (최신순)
     */
    List<WeeklyInsightEntity> findByMemberIdOrderByWeekStartDateDesc(Long memberId);

    /**
     * 특정 사용자의 분석 가능한 주차 목록 (주차별 그룹핑)
     */
    @Query("""
        SELECT DISTINCT w.weekStartDate 
        FROM WeeklyInsightEntity w 
        WHERE w.memberId = :memberId 
        ORDER BY w.weekStartDate DESC
        """)
    List<LocalDate> findDistinctWeekStartDatesByMemberIdOrderByWeekStartDateDesc(@Param("memberId") Long memberId);

    /**
     * 특정 기간 내 분석 결과 조회
     */
    @Query("""
        SELECT w FROM WeeklyInsightEntity w 
        WHERE w.memberId = :memberId 
        AND w.weekStartDate >= :startDate 
        AND w.weekStartDate <= :endDate 
        ORDER BY w.weekStartDate DESC
        """)
    List<WeeklyInsightEntity> findByMemberIdAndWeekStartDateBetween(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 사용자의 최신 분석 결과 조회
     */
    Optional<WeeklyInsightEntity> findTopByMemberIdOrderByWeekStartDateDesc(Long memberId);

    /**
     * 분석 결과 존재 여부 확인
     */
    boolean existsByMemberIdAndWeekStartDate(Long memberId, LocalDate weekStartDate);

    /**
     * 특정 타입의 분석 결과 존재 여부 확인
     */
    boolean existsByMemberIdAndWeekStartDateAndAnalysisType(
            Long memberId, LocalDate weekStartDate, AnalysisType analysisType);
}