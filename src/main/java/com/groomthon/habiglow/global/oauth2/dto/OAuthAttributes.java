package com.groomthon.habiglow.global.oauth2.dto;

import java.util.Map;

import com.groomthon.habiglow.global.oauth2.entity.SocialType;
import com.groomthon.habiglow.global.oauth2.userInfo.GoogleOAuth2UserInfo;
import com.groomthon.habiglow.global.oauth2.userInfo.KakaoOAuth2UserInfo;
import com.groomthon.habiglow.global.oauth2.userInfo.NaverOAuth2UserInfo;
import com.groomthon.habiglow.global.oauth2.userInfo.OAuth2UserInfo;

import lombok.Builder;
import lombok.Getter;

/**
 * 각 소셜에서 받아오는 데이터가 다르므로
 * 소셜별로 데이터를 받는 데이터를 분기 처리하는 DTO 클래스
 */

@Getter
public class OAuthAttributes {

	private String nameAttributeKey; // OAuth2 로그인 진행 시 키가 되는 필드 값, PK와 같은 의미
	private OAuth2UserInfo oauth2UserInfo; // 소셜 타입별 로그인 유저 정보(닉네임, 이메일, 프로필 사진 등등)
	private SocialType socialType; // 소셜 로그인 타입

	@Builder
	private OAuthAttributes(String nameAttributeKey, OAuth2UserInfo oauth2UserInfo, SocialType socialType) {
		this.nameAttributeKey = nameAttributeKey;
		this.oauth2UserInfo = oauth2UserInfo;
		this.socialType = socialType;
	}


	public static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.nameAttributeKey(userNameAttributeName)
			.oauth2UserInfo(new KakaoOAuth2UserInfo(attributes))
			.socialType(SocialType.KAKAO)
			.build();
	}

	public static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.nameAttributeKey(userNameAttributeName)
			.oauth2UserInfo(new GoogleOAuth2UserInfo(attributes))
			.socialType(SocialType.GOOGLE)
			.build();
	}

	public static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.nameAttributeKey(userNameAttributeName)
			.oauth2UserInfo(new NaverOAuth2UserInfo(attributes))
			.socialType(SocialType.NAVER)
			.build();
	}

	public String getEmail() {
		return oauth2UserInfo.getEmail();
	}

	public String getName() {
		return oauth2UserInfo.getNickname();
	}

	public String getSocialUniqueId() {
		return socialType.name() + "_" + oauth2UserInfo.getId();
	}
}
