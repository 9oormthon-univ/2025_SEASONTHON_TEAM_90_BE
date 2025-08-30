package com.groomthon.habiglow.domain.member.dto.request;

import java.util.List;

import org.hibernate.validator.constraints.URL;

import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 정보 부분 수정 요청 DTO")
public class UpdateMemberRequest {
	
	@Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하입니다.")
	@Schema(description = "회원 이름 (선택사항)", example = "홍길동")
	private String memberName;
	
	@URL(message = "올바른 URL 형식이 아닙니다.")
	@Schema(description = "프로필 이미지 URL (선택사항)", example = "https://example.com/profile.jpg")
	private String profileImageUrl;
	
	@Schema(description = "관심사 목록 (선택사항)", example = "[\"HEALTH\", \"LEARNING\"]")
	private List<RoutineCategory> interests;
}