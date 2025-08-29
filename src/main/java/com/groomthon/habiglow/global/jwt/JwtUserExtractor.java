package com.groomthon.habiglow.global.jwt;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtUserExtractor {

	private final JWTUtil jwtUtil;

	public Long extractUserId(HttpServletRequest request) {
		String token = extractTokenFromRequest(request);
		if (token == null) {
			throw new BaseException(ErrorCode.ACCESS_TOKEN_REQUIRED);
		}

		if (!jwtUtil.isAccessToken(token)) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		String userIdStr = jwtUtil.getId(token)
			.orElseThrow(() -> new BaseException(ErrorCode.INVALID_TOKEN));
		try {
			return Long.parseLong(userIdStr);
		} catch (NumberFormatException e) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}
	}

	public String extractUserEmail(HttpServletRequest request) {
		String token = extractTokenFromRequest(request);
		if (token == null) {
			throw new BaseException(ErrorCode.ACCESS_TOKEN_REQUIRED);
		}

		if (!jwtUtil.isAccessToken(token)) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		return jwtUtil.getEmail(token)
			.orElseThrow(() -> new BaseException(ErrorCode.INVALID_TOKEN));
	}

	private String extractTokenFromRequest(HttpServletRequest request) {
		return jwtUtil.extractAccessToken(request).orElse(null);
	}
}