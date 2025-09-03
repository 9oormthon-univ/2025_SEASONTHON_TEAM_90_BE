package com.groomthon.habiglow.domain.daily.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;

public interface DailyReflectionRepository extends JpaRepository<DailyReflectionEntity, Long> {

    // 단일 날짜 조회 (member.id + reflectionDate)
    Optional<DailyReflectionEntity> findByMemberIdAndReflectionDate(Long memberId, LocalDate date);

    // 주간 범위 조회 (member.id + reflectionDate BETWEEN, ASC 정렬)
    @Query("SELECT r FROM DailyReflectionEntity r " +
            "WHERE r.member.id = :memberId " +
            "AND r.reflectionDate BETWEEN :start AND :end " +
            "ORDER BY r.reflectionDate ASC")
    List<DailyReflectionEntity> findByMemberIdAndReflectionDateBetweenOrderByReflectionDateAsc(
            @Param("memberId") Long memberId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
