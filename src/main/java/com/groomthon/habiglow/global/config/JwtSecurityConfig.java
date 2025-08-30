package com.groomthon.habiglow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.groomthon.habiglow.domain.auth.service.BlacklistService;
<<<<<<< HEAD
=======
import com.groomthon.habiglow.domain.member.security.CustomUserDetailsService;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtSecurityConfig {

	private final JWTUtil jwtUtil;
	private final BlacklistService blacklistService;
<<<<<<< HEAD

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtUtil, blacklistService);
=======
	private final CustomUserDetailsService customUserDetailsService;

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtUtil, blacklistService, customUserDetailsService);
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
	}
}