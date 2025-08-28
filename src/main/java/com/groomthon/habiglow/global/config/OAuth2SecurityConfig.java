package com.groomthon.habiglow.global.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.stereotype.Component;

import com.groomthon.habiglow.global.oauth2.handler.OAuth2LoginFailureHandler;
import com.groomthon.habiglow.global.oauth2.handler.OAuth2LoginSuccessHandler;
import com.groomthon.habiglow.global.oauth2.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

	public void configureOAuth2Login(OAuth2LoginConfigurer<HttpSecurity> oauth2) {
		oauth2
			.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
			.successHandler(oAuth2LoginSuccessHandler)
			.failureHandler(oAuth2LoginFailureHandler);
	}
}