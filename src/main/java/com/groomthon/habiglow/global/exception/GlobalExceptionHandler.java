package com.groomthon.habiglow.global.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.groomthon.habiglow.global.advice.ParameterData;
import com.groomthon.habiglow.global.dto.CommonApiResponse;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. 커스텀 예외 처리
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<?> handleBaseException(BaseException e) {
		return ResponseEntity
			.status(e.getErrorCode().getStatus())
			.body(CommonApiResponse.fail(e.getErrorCode()));
	}

	// 2. Enum 변환 에러
	@ExceptionHandler(InvalidFormatException.class)
	public ResponseEntity<?> handleInvalidFormat(InvalidFormatException e) {

		return ResponseEntity
			.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
			.body(CommonApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
	}

	// 3. Bean Validation 실패 예외
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException e) {
		List<ParameterData> errors = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new ParameterData(
				error.getField(),
				error.getRejectedValue() != null ? error.getRejectedValue().toString() : "null",
				error.getDefaultMessage()
			))
			.collect(Collectors.toList());

		log.warn("Validation failed: {}", errors);

		return ResponseEntity
			.status(ErrorCode.PARAMETER_VALIDATION_ERROR.getStatus())
			.body(CommonApiResponse.failWithDetails(ErrorCode.PARAMETER_VALIDATION_ERROR, errors));
	}

	// 4. 회원 없음 예외
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException e) {
		return ResponseEntity
			.status(ErrorCode.MEMBER_NOT_FOUND.getStatus())
			.body(CommonApiResponse.fail(ErrorCode.MEMBER_NOT_FOUND));
	}
}