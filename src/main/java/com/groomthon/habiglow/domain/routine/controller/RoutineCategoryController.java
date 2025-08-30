package com.groomthon.habiglow.domain.routine.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.routine.dto.response.RoutineCategoryResponse;
import com.groomthon.habiglow.global.response.ApiSuccessCode;
import com.groomthon.habiglow.global.response.AutoApiResponse;
import com.groomthon.habiglow.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/routine-categories")
@AutoApiResponse
@Tag(name = "루틴 카테고리 API", description = "사용 가능한 루틴 카테고리 조회 API")
public class RoutineCategoryController {

	@Operation(
		summary = "루틴 카테고리 목록 조회",
		description = "회원이 선택할 수 있는 모든 루틴 카테고리 목록을 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping
	@SuccessCode(ApiSuccessCode.SUCCESS)
	public List<RoutineCategoryResponse> getAllCategories() {
		return RoutineCategoryResponse.fromAll();
	}
}