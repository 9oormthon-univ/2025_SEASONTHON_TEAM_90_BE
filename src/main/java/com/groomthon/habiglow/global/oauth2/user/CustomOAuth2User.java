package com.groomthon.habiglow.global.oauth2.user;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.groomthon.habiglow.global.oauth2.entity.SocialType;

/**
 * DefaultOAuth2User를 상속하고, email 필드를 추가로 가진다.
 * Role 시스템 제거로 단순화됨
 */
public class CustomOAuth2User extends DefaultOAuth2User {

	private final String memberId;
	private final String email;
	private final SocialType socialType;
	private final String socialUniqueId;

	public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
		Map<String, Object> attributes,
		String nameAttributeKey,
		String memberId, String email, SocialType socialType) {
		super(authorities, attributes, nameAttributeKey);
		this.memberId = memberId;
		this.email = email;
		this.socialType = socialType;
		this.socialUniqueId = socialType.name() + "_" + memberId;
	}

	@Override
	public String getName() {
		return super.getName(); // nameAttributeKey 기반으로 반환
	}

	public String getEmail() {
		return email;
	}


	public SocialType getSocialType() {
		return socialType;
	}

	public String getMemberId() {
		return memberId;
	}

	public String getSocialUniqueId() {
		return socialUniqueId;
	}
}