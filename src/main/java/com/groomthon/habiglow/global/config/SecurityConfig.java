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

	@Bean
	public RateLimitFilter rateLimitFilter(ConcurrentHashMap<String, RateLimiter> rateLimiterMap) {
		return new RateLimitFilter(rateLimiterMap);
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