package com.groomthon.habiglow.domain.routine.helper;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 루틴 도메인에서 공통으로 사용되는 헬퍼 메서드들을 제공하는 유틸리티 클래스
 * Repository 접근 로직을 중앙화하여 중복을 제거함
 */
@Component
@RequiredArgsConstructor
public class RoutineHelper {
    
    private final RoutineRepository routineRepository;
    private final MemberRepository memberRepository;

    /**
     * 멤버 ID로 멤버 엔티티를 조회합니다.
     * @param memberId 조회할 멤버 ID
     * @return MemberEntity
     * @throws BaseException 멤버를 찾을 수 없는 경우
     */
    public MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 루틴 ID와 멤버 ID로 루틴 엔티티를 조회합니다.
     * 보안을 위해 해당 멤버의 루틴만 조회 가능합니다.
     * @param routineId 조회할 루틴 ID
     * @param memberId 루틴 소유자의 멤버 ID
     * @return RoutineEntity
     * @throws BaseException 루틴을 찾을 수 없는 경우
     */
    public RoutineEntity findRoutineByIdAndMemberId(Long routineId, Long memberId) {
        return routineRepository.findByRoutineIdAndMember_Id(routineId, memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
    }

    /**
     * 루틴이 특정 멤버에게 속하는지 검증합니다.
     * @param routine 검증할 루틴
     * @param memberId 멤버 ID
     * @throws BaseException 루틴의 소유자가 아닌 경우
     */
    public void validateRoutineOwnership(RoutineEntity routine, Long memberId) {
        if (!routine.isOwnedBy(memberId)) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_FOUND);
        }
    }
}