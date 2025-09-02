package com.groomthon.habiglow.global.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.util.concurrent.RateLimiter;
import com.groomthon.habiglow.global.config.properties.SecurityProperties;
import com.groomthon.habiglow.global.dto.CommonApiResponse;
import com.groomthon.habiglow.global.jwt.JwtAuthenticationFilter;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final SecurityProperties securityProperties;
	private final CorsConfig corsConfig;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Bean
	public RateLimitFilter rateLimitFilter(ConcurrentHashMap<String, RateLimiter> rateLimiterMap) {
		return new RateLimitFilter(rateLimiterMap);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		RateLimitFilter rateLimitFilter) throws Exception {

		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(securityProperties.getPublicUrlsArray()).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(except -> except
				.authenticationEntryPoint((request, response, authException) -> {
					response.setStatus(HttpStatus.UNAUTHORIZED.value());
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.setCharacterEncoding("UTF-8");
					
					CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(ErrorCode.ACCESS_TOKEN_REQUIRED);
					response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
				})
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					response.setStatus(HttpStatus.FORBIDDEN.value());
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.setCharacterEncoding("UTF-8");
					
					CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(ErrorCode.ACCESS_DENIED);
					response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
				}));

		http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}