package com.groomthon.habiglow.global.oauth2.strategy;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SocialLoginStrategyManager {

	private final List<SocialLoginStrategy> strategies;

	/**
	 * 등록 ID에 맞는 전략을 찾아 속성을 추출합니다.
	 */
	public OAuthAttributes extractAttributes(String registrationId,
		String userNameAttributeName,
		Map<String, Object> attributes) {

		SocialLoginStrategy strategy = findStrategy(registrationId);
		return strategy.extractAttributes(userNameAttributeName, attributes);
	}

	/**
	 * 등록 ID에 맞는 소셜 타입을 반환합니다.
	 */
	public SocialType getSocialType(String registrationId) {
		SocialLoginStrategy strategy = findStrategy(registrationId);
		return strategy.getSocialType();
	}

	private SocialLoginStrategy findStrategy(String registrationId) {
		return strategies.stream()
			.filter(s -> s.supports(registrationId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"Unsupported social login type: " + registrationId));
	}
}
