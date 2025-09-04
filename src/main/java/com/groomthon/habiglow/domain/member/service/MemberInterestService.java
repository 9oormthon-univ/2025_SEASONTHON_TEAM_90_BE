package com.groomthon.habiglow.domain.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.member.dto.response.MemberInterestsResponse;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.entity.MemberInterest;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberInterestService {
	
	private final MemberRepository memberRepository;
	private final EntityManager entityManager;
	
	@Transactional
	public void updateInterests(Long memberId, List<RoutineCategory> newCategories) {
		
		// 1. 기존 관심사 직접 삭제
		entityManager.createQuery("DELETE FROM MemberInterest mi WHERE mi.member.id = :memberId")
			.setParameter("memberId", memberId)
			.executeUpdate();
		
		// 2. 영속성 컨텍스트 클리어
		entityManager.flush();
		entityManager.clear();

		// 3. 새로운 관심사 추가
		final MemberEntity finalMember = memberRepository.findById(memberId)
			.orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
		newCategories.forEach(category -> {
			MemberInterest interest = MemberInterest.of(finalMember, category);
			entityManager.persist(interest);
		});
		
		entityManager.flush();
		
		log.info("회원 관심사 업데이트 완료: memberId={}, interests={}", 
			memberId, newCategories);
	}
	
	public MemberInterestsResponse getMemberInterests(Long memberId) {
		MemberEntity member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
			
		return MemberInterestsResponse.from(member);
	}
	
	public boolean hasMemberInterests(Long memberId) {
		MemberEntity member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
			
		return !member.getInterestCategories().isEmpty();
	}
}