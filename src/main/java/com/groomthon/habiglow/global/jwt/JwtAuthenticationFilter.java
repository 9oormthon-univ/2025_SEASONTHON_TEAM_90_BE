package com.groomthon.habiglow.global.jwt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.domain.auth.service.BlacklistService;
import com.groomthon.habiglow.global.dto.CommonApiResponse;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JWTUtil jwtUtil;
	private final BlacklistService blacklistService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		jwtUtil.extractAccessToken(request)
			.filter(token -> validateAccessToken(token, response))
			.ifPresent(token -> {
				try {
					String email = jwtUtil.getEmail(token).orElse("");
					String userId = jwtUtil.getId(token).orElse("");

					Authentication authentication = new UsernamePasswordAuthenticationToken(
						userId, // principal: 사용자 ID
						null,   // credentials: JWT 토큰 기반이므로 null
						Collections.singletonList(new SimpleGrantedAuthority("SOCIAL_USER")) // authorities
					);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.info("JWT 기반 SecurityContext 저장 완료: email={}, userId={}", email, userId);
				} catch (Exception e) {
					log.warn("JWT 인증 처리 실패: {}", e.getMessage());
					sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
				}
			});

		filterChain.doFilter(request, response);
	}

	private boolean validateAccessToken(String token, HttpServletResponse response) {
		if (!jwtUtil.validateToken(token, "access")) {
			log.warn("토큰 검증 실패");
			sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
			return false;
		}

		if (blacklistService.isBlacklisted(token)) {
			log.warn("블랙리스트에 등록된 토큰입니다.");
			sendErrorResponse(response, ErrorCode.TOKEN_BLACKLISTED);
			return false;
		}

		return true;
	}

	private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) {
		try {
			response.setStatus(errorCode.getStatus());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			
			CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(errorCode);
			String jsonResponse = objectMapper.writeValueAsString(errorResponse);
			
			PrintWriter writer = response.getWriter();
			writer.write(jsonResponse);
			writer.flush();
		} catch (IOException e) {
			log.error("JWT 필터에서 에러 응답 전송 실패", e);
		}
	}
}