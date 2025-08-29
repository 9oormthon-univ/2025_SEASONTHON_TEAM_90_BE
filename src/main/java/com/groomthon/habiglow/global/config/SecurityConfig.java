package com.groomthon.habiglow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.groomthon.habiglow.global.config.properties.SecurityProperties;
import com.groomthon.habiglow.global.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final SecurityProperties securityProperties;
	private final CorsConfig corsConfig;
	private final OAuth2SecurityConfig oAuth2SecurityConfig;
	private final SessionRegistry sessionRegistry;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.maximumSessions(1) // 사용자당 최대 1개 세션
				.maxSessionsPreventsLogin(false) // 새 로그인시 기존 세션 만료
				.sessionRegistry(sessionRegistry)
				.and()
				.sessionFixation().migrateSession() // 세션 고정 공격 방지
				.invalidSessionUrl("/login?expired"))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(securityProperties.getPublicUrlsArray()).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(except -> except
				.authenticationEntryPoint((request, response, authException) ->
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")));

		// OAuth2 소셜 로그인만 활성화
		http.oauth2Login(oAuth2SecurityConfig::configureOAuth2Login);

		// JWT 인증 필터만 사용 (일반 로그인 필터 제거)
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}