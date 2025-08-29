package com.groomthon.habiglow.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiSuccessCode implements SuccessType {
	// 공통
	SUCCESS("S200", "성공"),

	// 인증 관련
	LOGIN_SUCCESS("S201", "로그인 성공"),
	LOGOUT_SUCCESS("S201", "로그아웃 성공"),
	TOKEN_REISSUE_SUCCESS("S202", "Access 토큰 재발급 성공"),
	TOKEN_REISSUE_FULL_SUCCESS("S203", "Access/Refresh 토큰 재발급 성공"),
	EMAIL_CHECK_OK("S204", "이메일 사용 가능"),
	SOCIAL_LOGIN_SUCCESS("S209", "소셜 로그인 성공"),

	// 회원 관련
	MEMBER_CREATED("S205", "회원가입 성공"),
	MEMBER_UPDATED("S206", "회원 정보 수정 성공"),
	MEMBER_DELETED("S207", "회원 삭제 성공"),
	MEMBER_VIEW("S208", "회원 정보 조회 성공"),

	// 루틴 관련
	ROUTINE_CREATED("S210", "루틴 생성 성공"),
	ROUTINE_VIEW("S211", "루틴 조회 성공"),
	ROUTINE_UPDATED("S212", "루틴 수정 성공"),
	ROUTINE_DELETED("S213", "루틴 삭제 성공"),
	ROUTINE_LIST_VIEW("S214", "루틴 목록 조회 성공");

	private final String code;
	private final String message;
}