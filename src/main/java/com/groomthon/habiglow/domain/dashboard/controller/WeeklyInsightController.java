package com.groomthon.habiglow.domain.dashboard.controller;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyInsightDto;
import com.groomthon.habiglow.domain.dashboard.service.WeeklyInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard/weekly")
@Tag(name = "주간 대시보드 API", description = "주간 AI 인사이트 및 통계를 관리하는 API")
@SecurityRequirement(name = "JWT") // Swagger에서 Authorize(베어러 토큰) 사용
public class WeeklyInsightController {

    private final WeeklyInsightService service;

    @Operation(
            summary = "특정 주차 인사이트 조회/생성",
            description = "주차의 시작일(월요일)을 기준으로 인사이트를 1회 생성 후 저장하고, 동일 입력이면 재사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = WeeklyInsightDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 거부"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/insight")
    public ResponseEntity<WeeklyInsightDto> getInsight(
            @Parameter(name = "weekStart", description = "주 시작일(월요일)", required = true, example = "2025-08-25", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Parameter(name = "force", description = "기존 저장이 있어도 재생성 여부", example = "false", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "false") boolean force,
            @Parameter(name = "memberId", description = "대상 사용자 ID (예: 더미는 1)", required = true, example = "1", in = ParameterIn.QUERY)
            @RequestParam Long memberId
    ) {
        return ResponseEntity.ok(service.getOrCreate(memberId, weekStart, force));
    }

    @Operation(
            summary = "지난 주 인사이트 조회/생성",
            description = "가장 최근 지난 주(월~일)의 인사이트를 조회/생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = WeeklyInsightDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 거부"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/insight/last-week")
    public ResponseEntity<WeeklyInsightDto> getLastWeekInsight(
            @Parameter(name = "force", description = "기존 저장이 있어도 재생성 여부", example = "false", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "false") boolean force,
            @Parameter(name = "memberId", description = "대상 사용자 ID (예: 더미는 1)", required = true, example = "1", in = ParameterIn.QUERY)
            @RequestParam Long memberId
    ) {
        return ResponseEntity.ok(service.getLastWeekOrCreate(memberId, force));
    }
}
