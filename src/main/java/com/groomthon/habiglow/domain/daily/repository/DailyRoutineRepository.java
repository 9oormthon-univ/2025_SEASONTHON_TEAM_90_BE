package com.groomthon.habiglow.domain.daily.repository;

import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyRoutineRepository extends JpaRepository<DailyRoutineEntity, Long> {

    // 주차 존재 여부: member 연관 + performedDate 범위
    boolean existsByMember_IdAndPerformedDateBetween(Long memberId, LocalDate start, LocalDate end);

    // 주간 매핑용
    List<DailyRoutineEntity> findByMember_IdAndPerformedDateBetween(Long memberId, LocalDate start, LocalDate end);

    // ⬇️ 서비스가 호출하는 이름을 그대로 맞춰줌 (파생쿼리 대신 JPQL)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DailyRoutineEntity r where r.member.id = :memberId and r.performedDate = :date")
    void deleteByMemberIdAndPerformedDate(@Param("memberId") Long memberId,
                                          @Param("date") LocalDate date);

    // 조인 페치 버전
    @Query("select r from DailyRoutineEntity r " +
            "join fetch r.routine " +
            "where r.member.id = :memberId and r.performedDate = :date")
    List<DailyRoutineEntity> findByMemberIdAndPerformedDateWithRoutine(@Param("memberId") Long memberId,
                                                                       @Param("date") LocalDate date);
}
