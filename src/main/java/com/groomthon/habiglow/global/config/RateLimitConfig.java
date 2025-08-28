package com.groomthon.habiglow.global.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.RateLimiter;

@Configuration
public class RateLimitConfig {

	@Bean
	public ConcurrentHashMap<String, RateLimiter> rateLimiterMap() {
		return new ConcurrentHashMap<>();
	}
}