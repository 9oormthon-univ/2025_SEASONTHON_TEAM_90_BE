package com.groomthon.habiglow.global.oauth2.userInfo;

import java.util.Map;

public class NaverOAuth2UserInfo extends com.groomthon.habiglow.global.oauth2.userInfo.OAuth2UserInfo {

	public NaverOAuth2UserInfo(Map<String, Object> attributes) {
		super(attributes);
	}

	private Map<String, Object> getResponse() {
		return (Map<String, Object>) attributes.get("response");
	}

	@Override
	public String getId() {
		Map<String, Object> response = getResponse();
		return response != null ? (String) response.get("id") : null;
	}

	@Override
	public String getNickname() {
		Map<String, Object> response = getResponse();
		return response != null ? (String) response.get("nickname") : null;
	}

	@Override
	public String getEmail() {
		Map<String, Object> response = getResponse();
		return response != null ? (String) response.get("email") : null;
	}

	@Override
	public String getImageUrl() {
		Map<String, Object> response = getResponse();
		return response != null ? (String) response.get("profile_image") : null;
	}
}
