package com.groomthon.habiglow.domain.daily.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;

public interface DailyReflectionRepository extends JpaRepository<DailyReflectionEntity, Long> {
    
    Optional<DailyReflectionEntity> findByMemberIdAndReflectionDate(Long memberId, LocalDate date);

}