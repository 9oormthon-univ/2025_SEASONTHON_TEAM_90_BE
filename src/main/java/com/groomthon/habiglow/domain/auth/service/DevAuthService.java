package com.groomthon.habiglow.domain.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.request.MockLoginRequest;
import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.global.jwt.JWTUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DevAuthService {

	private final MemberRepository memberRepository;
	private final JWTUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;

	public void mockRegister(MockLoginRequest request) {
		String socialUniqueId = request.getSocialType().name() + "_" + request.getMockSocialId();

		// 중복 체크
		if (memberRepository.findBySocialUniqueId(socialUniqueId).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 socialUniqueId입니다: " + socialUniqueId);
		}

		if (memberRepository.findByMemberEmail(request.getEmail()).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + request.getEmail());
		}

		MemberEntity mockUser = MemberEntity.createSocialMember(
			request.getEmail(),
			request.getName(),
			request.getSocialType(),
			request.getMockSocialId(),
			null); // Mock user는 프로필 이미지 URL 없음

		memberRepository.save(mockUser);

		log.info("테스트 사용자 생성 완료: {}, socialUniqueId: {}",
			request.getEmail(), socialUniqueId);
	}

	public TokenResponse mockLogin(MockLoginRequest request, HttpServletResponse response) {
		MemberEntity mockUser = findExistingMockUser(request);

		String memberId = mockUser.getId().toString();
		String accessToken = jwtUtil.createAccessTokenSafe(memberId, mockUser.getMemberEmail(), mockUser.getSocialUniqueId());
		String refreshToken = jwtUtil.createRefreshTokenSafe(memberId, mockUser.getMemberEmail(), mockUser.getSocialUniqueId());

		refreshTokenService.saveRefreshToken(mockUser.getId(), refreshToken);
		
		response.setHeader("Authorization", "Bearer " + accessToken);
		response.setHeader("Set-Cookie", jwtUtil.createRefreshTokenCookie(refreshToken).toString());

		long expirySeconds = jwtUtil.getAccessTokenExpiration() / 1000;

		log.info("개발용 토큰 발급 완료 - 사용자: {}, socialUniqueId: {}",
			mockUser.getMemberEmail(), mockUser.getSocialUniqueId());

		return TokenResponse.withRefresh(accessToken, expirySeconds);
	}

	private MemberEntity findExistingMockUser(MockLoginRequest request) {
		String socialUniqueId = request.getSocialType().name() + "_" + request.getMockSocialId();

		return memberRepository.findBySocialUniqueId(socialUniqueId)
			.or(() -> memberRepository.findByMemberEmail(request.getEmail()))
			.orElseThrow(() -> new IllegalArgumentException(
				"존재하지 않는 사용자입니다. 먼저 회원가입을 진행해주세요. email: " + request.getEmail()));
	}

}