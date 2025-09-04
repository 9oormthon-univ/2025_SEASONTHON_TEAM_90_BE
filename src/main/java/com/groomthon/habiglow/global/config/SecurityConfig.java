package com.groomthon.habiglow.global.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.google.common.util.concurrent.RateLimiter;
import com.groomthon.habiglow.global.config.properties.SecurityProperties;
import com.groomthon.habiglow.domain.auth.service.BlacklistService;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.jwt.JwtAuthenticationFilter;
import com.groomthon.habiglow.global.security.JwtAccessDeniedHandler;
import com.groomthon.habiglow.global.security.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final SecurityProperties securityProperties;
	private final CorsConfig corsConfig;
	private final JWTUtil jwtUtil;
	private final BlacklistService blacklistService;

	@Bean
	public RateLimitFilter rateLimitFilter(ConcurrentHashMap<String, RateLimiter> rateLimiterMap, 
			com.groomthon.habiglow.global.util.SecurityResponseUtils responseUtils) {
		return new RateLimitFilter(rateLimiterMap, responseUtils);
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtUtil, blacklistService);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		RateLimitFilter rateLimitFilter,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		JwtAccessDeniedHandler jwtAccessDeniedHandler) throws Exception {

		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers
				// X-Frame-Options: DENY (클릭재킹 방지)
				.frameOptions(frameOptions -> frameOptions.deny())
				// X-Content-Type-Options: nosniff (MIME 타입 스니핑 방지)
				.contentTypeOptions(contentTypeOptions -> {})
				// HSTS 헤더 설정 (HTTPS 강제)
				.httpStrictTransportSecurity(hsts -> hsts
					.maxAgeInSeconds(31536000) // 1년
					.includeSubDomains(true)
					.preload(true)
				)
				// 추가 보안 헤더들
				.addHeaderWriter((request, response) -> {
					// X-XSS-Protection 헤더 (레거시 브라우저 지원용)
					response.setHeader("X-XSS-Protection", "1; mode=block");
					// Referrer Policy 헤더
					response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
					// Permissions Policy 헤더 (보안 강화)
					response.setHeader("Permissions-Policy", 
						"camera=(), microphone=(), geolocation=(), interest-cohort=()");
				})
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(securityProperties.getPublicUrlsArray()).permitAll()
				.requestMatchers("/error").permitAll() // 에러 페이지 재귀 방지
				.anyRequest().authenticated())
			.exceptionHandling(except -> except
				.authenticationEntryPoint(jwtAuthenticationEntryPoint) // 커스텀 EntryPoint
				.accessDeniedHandler(jwtAccessDeniedHandler)); // 커스텀 AccessDeniedHandler

		http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
	
}