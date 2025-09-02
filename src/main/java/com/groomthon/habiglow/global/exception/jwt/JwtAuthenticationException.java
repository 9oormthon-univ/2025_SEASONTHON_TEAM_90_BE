package com.groomthon.habiglow.global.exception.jwt;

import org.springframework.security.core.AuthenticationException;

import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.Getter;

@Getter
public abstract class JwtAuthenticationException extends AuthenticationException {
	
	private final ErrorCode errorCode;
	
	public JwtAuthenticationException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
	
	public JwtAuthenticationException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}