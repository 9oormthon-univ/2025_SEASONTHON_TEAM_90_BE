package com.groomthon.habiglow.global.exception;

import com.groomthon.habiglow.global.response.ErrorType;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
	private final ErrorType errorCode;

	public BaseException(ErrorType errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}