package com.groomthon.habiglow.global.jwt;

import org.springframework.stereotype.Component;

import com.groomthon.habiglow.domain.auth.service.TokenValidator;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtMemberExtractor {

	private final JWTUtil jwtUtil;
	private final TokenValidator tokenValidator;

	public Long extractMemberId(HttpServletRequest request) {
		String token = extractTokenFromRequest(request);
		if (token == null) {
			throw new BaseException(ErrorCode.ACCESS_TOKEN_REQUIRED);
		}

		TokenValidator.TokenValidationResult validation = tokenValidator.validateAccessToken(token);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		try {
			return Long.parseLong(validation.getMemberId());
		} catch (NumberFormatException e) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}
	}

	public String extractMemberEmail(HttpServletRequest request) {
		String token = extractTokenFromRequest(request);
		if (token == null) {
			throw new BaseException(ErrorCode.ACCESS_TOKEN_REQUIRED);
		}

		TokenValidator.TokenValidationResult validation = tokenValidator.validateAccessToken(token);
		if (!validation.isValid()) {
			throw new BaseException(ErrorCode.INVALID_TOKEN);
		}

		return validation.getEmail();
	}

	private String extractTokenFromRequest(HttpServletRequest request) {
		return jwtUtil.extractAccessToken(request).orElse(null);
	}
}