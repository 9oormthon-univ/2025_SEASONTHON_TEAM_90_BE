package com.groomthon.habiglow.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.member.dto.response.MemberResponse;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용
public class MemberService {
	private final MemberRepository memberRepository;

	@Transactional
	public MemberEntity findOrCreateSocialMember(OAuthAttributes attributes) {
		return memberRepository.findBySocialUniqueIdAndSocialType(
				attributes.getSocialUniqueId(), 
				attributes.getSocialType()
			)
			.orElseGet(() -> createNewSocialMember(attributes));
	}

	private MemberEntity createNewSocialMember(OAuthAttributes attributes) {
		log.info("새로운 소셜 사용자 생성: email={}, socialType={}", 
				attributes.getEmail(), attributes.getSocialType());
		
		MemberEntity newMember = MemberEntity.builder()
			.memberEmail(attributes.getEmail())
			.memberName(attributes.getName())
			.socialType(attributes.getSocialType())
			.socialUniqueId(attributes.getSocialUniqueId())
			.build();
			
		return memberRepository.save(newMember);
	}


	public MemberResponse findById(Long id) {
		return memberRepository.findById(id)
			.map(MemberResponse::fromEntity)
			.orElseThrow(() -> memberNotFound());
	}



	@Transactional
	public void deleteById(Long id) {
		if (!memberRepository.existsById(id)) {
			throw memberNotFound();
		}
		memberRepository.deleteById(id);
	}


	private BaseException memberNotFound() {
		return new BaseException(ErrorCode.MEMBER_NOT_FOUND);
	}
}