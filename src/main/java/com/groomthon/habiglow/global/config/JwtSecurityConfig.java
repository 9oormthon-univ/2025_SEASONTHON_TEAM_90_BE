package com.groomthon.habiglow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.groomthon.habiglow.domain.auth.service.BlacklistService;
import com.groomthon.habiglow.global.config.properties.SecurityProperties;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtSecurityConfig {

	private final JWTUtil jwtUtil;
	private final BlacklistService blacklistService;
	private final SecurityProperties securityProperties;

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtUtil, blacklistService, securityProperties);
	}
}