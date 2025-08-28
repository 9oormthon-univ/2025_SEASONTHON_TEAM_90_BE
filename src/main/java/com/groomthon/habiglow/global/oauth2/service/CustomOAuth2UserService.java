package com.groomthon.habiglow.global.oauth2.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;
import com.groomthon.habiglow.global.oauth2.strategy.SocialLoginStrategyManager;
import com.groomthon.habiglow.global.oauth2.user.CustomOAuth2User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final MemberRepository userRepository;
	private final SocialLoginStrategyManager strategyManager;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		log.info("CustomOAuth2UserService.loadUser() 실행 - OAuth2 로그인 요청 진입");

		OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		String userNameAttributeName = userRequest.getClientRegistration()
			.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
		Map<String, Object> attributes = oAuth2User.getAttributes();

		OAuthAttributes extractAttributes = strategyManager.extractAttributes(
			registrationId, userNameAttributeName, attributes);

		SocialType socialType = strategyManager.getSocialType(registrationId);
		MemberEntity user = findOrCreateUser(extractAttributes, socialType);

		return new CustomOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority("SOCIAL_USER")),
			attributes,
			extractAttributes.getNameAttributeKey(),
			String.valueOf(user.getId()),
			user.getMemberEmail(),
			user.getSocialType()
		);
	}


	private MemberEntity findOrCreateUser(OAuthAttributes attributes, SocialType socialType) {
		String socialId = attributes.getOauth2UserInfo().getId();
		String socialUniqueId = socialType.name() + "_" + socialId;

		log.info("소셜 사용자 조회/생성: socialUniqueId={}", socialUniqueId);

		// 1. socialUniqueId로 기존 사용자 확인 (플랫폼별 완전 분리)
		return userRepository.findBySocialUniqueId(socialUniqueId)
			.orElseGet(() -> {
				log.info("신규 소셜 사용자 생성: socialUniqueId={}", socialUniqueId);
				return saveUser(attributes, socialType);
			});
	}


	private MemberEntity saveUser(OAuthAttributes attributes, SocialType socialType) {
		MemberEntity user = attributes.toEntity(socialType);
		return userRepository.save(user);
	}
}