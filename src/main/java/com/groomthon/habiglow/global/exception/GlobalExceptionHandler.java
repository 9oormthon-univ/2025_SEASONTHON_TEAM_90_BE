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
import com.groomthon.habiglow.domain.daily.exception.DailyRecordValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. 커스텀 예외 처리
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<?> handleBaseException(BaseException e) {
		log.warn("BaseException: {} - {}", e.getErrorCode().getCode(), e.getMessage());
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
				maskSensitiveValue(error.getField(), error.getRejectedValue()),
				error.getDefaultMessage()
			))
			.collect(Collectors.toList());

		log.warn("Validation failed for fields: {}", 
			errors.stream().map(ParameterData::getKey).collect(Collectors.toList()));

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

	// 5. 일반적인 IllegalArgument 예외
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
		log.warn("IllegalArgumentException: {}", e.getMessage());
		return ResponseEntity
			.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
			.body(CommonApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
	}

	// 6. 일반적인 런타임 예외
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
		log.error("Unexpected RuntimeException", e);
		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(CommonApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	// 7. 일일 기록 검증 예외
	@ExceptionHandler(DailyRecordValidationException.class)
	public ResponseEntity<?> handleDailyRecordValidation(DailyRecordValidationException e) {
		log.warn("Daily record validation failed for routines: {}", 
			e.getInvalidRoutines().stream()
				.map(error -> error.getRoutineId() + ":" + error.getReason())
				.collect(Collectors.toList()));
		
		return ResponseEntity
			.status(e.getErrorCode().getStatus())
			.body(CommonApiResponse.failWithDetails(e.getErrorCode(), e.getInvalidRoutines()));
	}

	// 8. 모든 예외의 최종 핸들러
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneralException(Exception e) {
		log.error("Unexpected Exception", e);
		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(CommonApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private String maskSensitiveValue(String fieldName, Object value) {
		if (value == null) {
			return "null";
		}
		
		String stringValue = value.toString();
		String lowerFieldName = fieldName.toLowerCase();
		
		// 민감 정보 필드들을 마스킹
		if (lowerFieldName.contains("password") || 
			lowerFieldName.contains("token") ||
			lowerFieldName.contains("secret")) {
			return "***";
		}
		
		return stringValue;
	}
}