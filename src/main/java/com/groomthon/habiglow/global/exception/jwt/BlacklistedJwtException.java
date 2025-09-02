package com.groomthon.habiglow.global.exception.jwt;

import com.groomthon.habiglow.global.response.ErrorCode;

public class BlacklistedJwtException extends JwtAuthenticationException {
	
	public BlacklistedJwtException() {
		super(ErrorCode.TOKEN_BLACKLISTED);
	}
	
	public BlacklistedJwtException(Throwable cause) {
		super(ErrorCode.TOKEN_BLACKLISTED, cause);
	}
}