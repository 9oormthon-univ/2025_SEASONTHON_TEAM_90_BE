package com.groomthon.habiglow.global.oauth2.strategy;

import java.util.Map;

import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

/**
 * 소셜 로그인별 처리를 위한 Strategy 인터페이스
 */
public interface SocialLoginStrategy {

	/**
	 * 이 전략이 처리할 수 있는 소셜 타입을 반환
	 */
	SocialType getSocialType();

	/**
	 * 소셜 로그인 응답에서 사용자 속성을 추출
	 */
	OAuthAttributes extractAttributes(String userNameAttributeName, Map<String, Object> attributes);

	/**
	 * 등록 ID로부터 이 전략을 사용할지 판단
	 */
	boolean supports(String registrationId);
}