package com.groomthon.habiglow.domain.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.request.MockLoginRequest;
import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.jwt.JwtTokenService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "local"}) // 개발 환경에서만 활성화
@Transactional
public class DevAuthService {

	private final MemberRepository memberRepository;
	private final JwtTokenService jwtTokenService;
	private final JWTUtil jwtUtil;

	public void mockRegister(MockLoginRequest request) {
		String socialUniqueId = request.getSocialType().name() + "_" + request.getMockSocialId();

		// 중복 체크
		if (memberRepository.findBySocialUniqueId(socialUniqueId).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 socialUniqueId입니다: " + socialUniqueId);
		}

		if (memberRepository.findByMemberEmail(request.getEmail()).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + request.getEmail());
		}

		// 새 사용자 생성
		MemberEntity mockUser = MemberEntity.createSocialMember(
			request.getEmail(),
			request.getName(),
			request.getSocialType(),
			request.getMockSocialId());

		memberRepository.save(mockUser);

		log.info("테스트 사용자 생성 완료: {}, socialUniqueId: {}",
			request.getEmail(), socialUniqueId);
	}

	public TokenResponse mockLogin(MockLoginRequest request, HttpServletResponse response) {
		// 기존 사용자 조회만 수행
		MemberEntity mockUser = findExistingMockUser(request);

		// JWT 토큰 발급
		jwtTokenService.issueTokens(response, mockUser);

		// Access Token 추출하여 응답
		String accessToken = extractAccessTokenFromResponse(response);
		long expirySeconds = jwtUtil.getAccessTokenExpiration() / 1000;

		log.info("개발용 토큰 발급 완료 - 사용자: {}, socialUniqueId: {}",
			mockUser.getMemberEmail(), mockUser.getSocialUniqueId());

		return TokenResponse.withRefresh(accessToken, expirySeconds);
	}

	private MemberEntity findExistingMockUser(MockLoginRequest request) {
		String socialUniqueId = request.getSocialType().name() + "_" + request.getMockSocialId();

		// socialUniqueId로 먼저 조회
		return memberRepository.findBySocialUniqueId(socialUniqueId)
			.or(() -> memberRepository.findByMemberEmail(request.getEmail()))
			.orElseThrow(() -> new IllegalArgumentException(
				"존재하지 않는 사용자입니다. 먼저 회원가입을 진행해주세요. email: " + request.getEmail()));
	}

	private String extractAccessTokenFromResponse(HttpServletResponse response) {
		String authHeader = response.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		return null;
	}
}