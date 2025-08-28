package com.groomthon.habiglow.global.oauth2.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;
import com.groomthon.habiglow.global.oauth2.userInfo.GoogleOAuth2UserInfo;

@Component
public class GoogleLoginStrategy implements SocialLoginStrategy {

	@Override
	public SocialType getSocialType() {
		return SocialType.GOOGLE;
	}

	@Override
	public OAuthAttributes extractAttributes(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.nameAttributeKey(userNameAttributeName)
			.oauth2UserInfo(new GoogleOAuth2UserInfo(attributes))
			.build();
	}

	@Override
	public boolean supports(String registrationId) {
		return "google".equals(registrationId);
	}
}