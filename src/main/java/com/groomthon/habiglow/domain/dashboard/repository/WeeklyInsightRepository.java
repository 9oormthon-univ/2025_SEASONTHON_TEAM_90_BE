package com.groomthon.habiglow.domain.dashboard.repository;

import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyInsightRepository extends JpaRepository<WeeklyInsightEntity, Long> {
    Optional<WeeklyInsightEntity> findByMemberIdAndWeekStart(Long memberId, LocalDate weekStart);
}
