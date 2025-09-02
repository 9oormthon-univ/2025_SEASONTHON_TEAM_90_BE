package com.groomthon.habiglow.global.exception.jwt;

import com.groomthon.habiglow.global.response.ErrorCode;

public class InvalidJwtSignatureException extends JwtAuthenticationException {
	
	public InvalidJwtSignatureException() {
		super(ErrorCode.INVALID_TOKEN);
	}
	
	public InvalidJwtSignatureException(Throwable cause) {
		super(ErrorCode.INVALID_TOKEN, cause);
	}
}