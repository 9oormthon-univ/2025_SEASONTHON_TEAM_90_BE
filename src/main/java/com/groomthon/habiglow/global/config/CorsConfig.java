package com.groomthon.habiglow.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.groomthon.habiglow.global.config.properties.SecurityProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

	private final SecurityProperties securityProperties;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		return request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOriginPatterns(List.of("*"));
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
			config.setAllowCredentials(true);
			config.setAllowedHeaders(List.of("*"));
			config.setExposedHeaders(List.of("Set-Cookie", "Content-Type"));
			config.setMaxAge(securityProperties.getCorsMaxAge());
			return config;
		};
	}
}