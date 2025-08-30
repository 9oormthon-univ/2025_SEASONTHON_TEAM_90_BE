package com.groomthon.habiglow.global.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorType {
	// 공통 오류 (COMMON)
	INVALID_INPUT_VALUE("COMMON001", "잘못된 입력값입니다", HttpStatus.BAD_REQUEST.value()),
	PARAMETER_VALIDATION_ERROR("COMMON002", "파라미터 검증에 실패했습니다", HttpStatus.BAD_REQUEST.value()),
	INTERNAL_SERVER_ERROR("COMMON999", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR.value()),

	// 인증 관련 오류 (AUTH)
	LOGIN_FAIL("AUTH001", "이메일 또는 비밀번호가 틀렸습니다", HttpStatus.UNAUTHORIZED.value()),
	INVALID_SOCIAL_TOKEN("AUTH002", "유효하지 않은 소셜 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	SOCIAL_LOGIN_ERROR("AUTH003", "소셜 로그인 처리 중 오류가 발생했습니다", HttpStatus.UNAUTHORIZED.value()),
	OAUTH2_LOGIN_FAILED("AUTH004", "소셜 로그인에 실패했습니다. 다시 시도해주세요.", HttpStatus.UNAUTHORIZED.value()),

	// 토큰 관련 오류 (TOKEN)
	TOKEN_MALFORMED("TOKEN001", "잘못된 형식의 토큰입니다", HttpStatus.BAD_REQUEST.value()),
	INVALID_TOKEN("TOKEN002", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	TOKEN_EXPIRED("TOKEN003", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED.value()),
	REFRESH_TOKEN_NOT_FOUND("TOKEN004", "리프레시 토큰을 찾을 수 없습니다", HttpStatus.UNAUTHORIZED.value()),
	ACCESS_TOKEN_REQUIRED("TOKEN005", "액세스 토큰이 필요합니다", HttpStatus.UNAUTHORIZED.value()),
	TOKEN_BLACKLISTED("TOKEN006", "차단된 토큰입니다", HttpStatus.UNAUTHORIZED.value()),

	// 회원 관련 오류 (MEMBER)
	MEMBER_NOT_FOUND("MEMBER001", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	DUPLICATE_EMAIL("MEMBER002", "이미 가입된 이메일입니다", HttpStatus.CONFLICT.value()),

	// 루틴 관련 오류 (ROUTINE)
	ROUTINE_NOT_FOUND("ROUTINE001", "루틴을 찾을 수 없습니다", HttpStatus.NOT_FOUND.value()),
	ROUTINE_GROWTH_MODE_DISABLED("ROUTINE002", "성장 모드가 비활성화된 루틴입니다", HttpStatus.BAD_REQUEST.value()),
	ROUTINE_INVALID_TITLE("ROUTINE003", "루틴 제목은 필수이며 100자 이하여야 합니다", HttpStatus.BAD_REQUEST.value()),
	ROUTINE_INVALID_CATEGORY("ROUTINE004", "루틴 카테고리는 필수입니다", HttpStatus.BAD_REQUEST.value()),
	ROUTINE_INVALID_GROWTH_SETTINGS("ROUTINE005", "성장 모드 설정이 올바르지 않습니다", HttpStatus.BAD_REQUEST.value()),
	ROUTINE_CANNOT_INCREASE_TARGET("ROUTINE006", "목표치를 증가시킬 수 없는 상태입니다", HttpStatus.BAD_REQUEST.value()),

	// 보안 관련 오류 (SECURITY)
	TOO_MANY_REQUESTS("SECURITY001", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS.value());

	private final String code;
	private final String message;
	private final int status;

	public int getHttpCode() {
		return this.status;
	}
}
