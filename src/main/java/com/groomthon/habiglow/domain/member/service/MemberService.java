package com.groomthon.habiglow.domain.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.member.dto.response.MemberResponse;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용
// @Profile("disabled") 제거 - UserApiController에서 필요하므로 활성화 유지
public class MemberService {
	private final MemberRepository memberRepository;




	public List<MemberResponse> findAll() {
		return memberRepository.findAll()
			.stream()
			.map(MemberResponse::fromEntity)
			.toList();
	}

	public MemberResponse findById(Long id) {
		return memberRepository.findById(id)
			.map(MemberResponse::fromEntity)
			.orElseThrow(() -> memberNotFound());
	}

	public MemberResponse findMemberForUpdate(String myEmail) {
		return memberRepository.findByMemberEmail(myEmail)
			.map(MemberResponse::fromEntity)
			.orElseThrow(() -> memberNotFound());
	}


	@Transactional // 쓰기 작업
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