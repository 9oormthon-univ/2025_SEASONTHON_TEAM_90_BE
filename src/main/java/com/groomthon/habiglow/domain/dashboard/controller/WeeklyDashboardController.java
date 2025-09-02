package com.groomthon.habiglow.domain.dashboard.controller;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyDashboardDto;
import com.groomthon.habiglow.domain.dashboard.service.WeeklyDashboardService;
import com.groomthon.habiglow.global.dto.CommonApiResponse;
import com.groomthon.habiglow.global.response.ApiSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/dashboard/weekly")
@RequiredArgsConstructor
@Tag(
        name = "Dashboard - Weekly View",
        description = "주간 대시보드(통계/감정/네비). 지난 주는 dummy-data 프로필 시 더미, 그 외 실데이터."
)
public class WeeklyDashboardController {
    private final WeeklyDashboardService weeklyDashboardService;

    @GetMapping("/stats")
    @Operation(
            operationId = "getWeeklyStats",
            summary = "주간 대시보드 집계 조회",
            description = "상단 KPI, 감정 분포, 요일별 완료율, 네비게이션 정보를 반환합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            schema = @Schema(implementation = WeeklyDashboardDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "예시(지난 주, 더미 적용)",
                                            value = """
                        {
                          "period": {
                            "weekStart":"2025-08-25",
                            "weekEnd":"2025-08-31",
                            "label":"8월 4째 주",
                            "currentWeek": false,
                            "complete": true,
                            "nav": {"hasPrev":true,"hasNext":true,"prevWeekStart":"2025-08-18","nextWeekStart":"2025-09-01"}
                          },
                          "metrics": {
                            "totalRoutines": 6,
                            "overall": {"done": 21, "total": 42, "rate": 50.0},
                            "categories": [
                              {"code":"HEALTH","label":"건강","done":8,"total":12,"rate":66.7}
                            ]
                          },
                          "emotionDistribution":{"HAPPY":3,"SOSO":2,"SAD":1,"MAD":0},
                          "dailyCompletion":[
                            {"date":"2025-08-25","done":3,"total":6,"rate":50.0,"mood":"HAPPY","future":false}
                          ]
                        }
                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public CommonApiResponse<WeeklyDashboardDto> getWeeklyView(
            @Parameter(description = "(임시) 사용자 ID. 추후 @AuthUser로 대체 예정")
            @RequestParam Long memberId,
            @Parameter(description = "주 시작일(월요일 기준). 예: 2025-08-25")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        var dto = weeklyDashboardService.getView(memberId, weekStart);
        return CommonApiResponse.success(ApiSuccessCode.SUCCESS, dto);
    }
}
