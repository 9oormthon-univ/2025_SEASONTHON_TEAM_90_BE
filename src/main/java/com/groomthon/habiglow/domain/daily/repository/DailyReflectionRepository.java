package com.groomthon.habiglow.domain.daily.repository;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyReflectionRepository extends JpaRepository<DailyReflectionEntity, Long> {

    // 주차 존재 여부: member 연관 + reflectionDate 범위
    boolean existsByMember_IdAndReflectionDateBetween(Long memberId, LocalDate start, LocalDate end);

    // available-weeks 계산용 (날짜만 가져와 Collector에서 주차 문자열로 변환)
    @Query("select r.reflectionDate from DailyReflectionEntity r where r.member.id = :memberId")
    List<LocalDate> findAllDatesByMemberId(@Param("memberId") Long memberId);

    // 주간 매핑용
    List<DailyReflectionEntity> findByMember_IdAndReflectionDateBetween(Long memberId, LocalDate start, LocalDate end);

    // 서비스가 호출하는 이름을 그대로 맞춰줌
    @Query("select r from DailyReflectionEntity r " +
            "where r.member.id = :memberId and r.reflectionDate = :date")
    Optional<DailyReflectionEntity> findByMemberIdAndReflectionDate(@Param("memberId") Long memberId,
                                                                    @Param("date") LocalDate date);


}
