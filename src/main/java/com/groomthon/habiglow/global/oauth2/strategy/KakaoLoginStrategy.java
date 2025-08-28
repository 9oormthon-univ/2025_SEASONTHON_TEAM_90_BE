package com.groomthon.habiglow.global.oauth2.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;
import com.groomthon.habiglow.global.oauth2.userInfo.KakaoOAuth2UserInfo;

@Component
public class KakaoLoginStrategy implements SocialLoginStrategy {

	@Override
	public SocialType getSocialType() {
		return SocialType.KAKAO;
	}

	@Override
	public OAuthAttributes extractAttributes(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.nameAttributeKey(userNameAttributeName)
			.oauth2UserInfo(new KakaoOAuth2UserInfo(attributes))
			.build();
	}

	@Override
	public boolean supports(String registrationId) {
		return "kakao".equals(registrationId);
	}
}
