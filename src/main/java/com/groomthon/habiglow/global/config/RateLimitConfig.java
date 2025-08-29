package com.groomthon.habiglow.global.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Rate Limiting을 위한 설정 클래스
 * IP별 요청 제한을 관리하는 Map을 제공
 */
@Configuration
public class RateLimitConfig {

	@Bean
	public ConcurrentHashMap<String, RateLimiter> rateLimiterMap() {
		return new ConcurrentHashMap<>();
	}
}