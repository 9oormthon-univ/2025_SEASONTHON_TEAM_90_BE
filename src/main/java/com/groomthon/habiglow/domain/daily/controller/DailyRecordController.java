package com.groomthon.habiglow.domain.daily.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groomthon.habiglow.domain.daily.dto.request.SaveDailyRecordRequest;
import com.groomthon.habiglow.domain.daily.dto.response.DailyRecordResponse;
import com.groomthon.habiglow.domain.daily.facade.DailyRecordFacade;
import com.groomthon.habiglow.global.jwt.JwtMemberExtractor;
import com.groomthon.habiglow.global.response.AutoApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/daily-records")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "일일 기록 관리 API", description = "루틴 수행 기록과 회고를 관리하는 API")
public class DailyRecordController {
    
    private final DailyRecordFacade dailyRecordFacade;
    private final JwtMemberExtractor jwtMemberExtractor;
    
    @Operation(
        summary = "일일 기록 저장",
        description = "특정 날짜의 루틴 수행 기록과 회고를 저장합니다. 당일 데이터만 수정 가능합니다."
    )
    @ApiResponse(responseCode = "200", description = "저장 성공")
    @PostMapping("/{date}")
    @PreAuthorize("hasRole('USER')")
    public DailyRecordResponse saveDailyRecord(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestBody @Valid SaveDailyRecordRequest request,
            HttpServletRequest httpRequest) {
        
        Long memberId = jwtMemberExtractor.extractMemberId(httpRequest);
        return dailyRecordFacade.saveDailyRecord(memberId, date, request);
    }
    
    @Operation(
        summary = "특정 날짜 기록 조회",
        description = "특정 날짜의 루틴 수행 기록과 회고를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{date}")
    @PreAuthorize("hasRole('USER')")
    public DailyRecordResponse getDailyRecord(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            HttpServletRequest httpRequest) {
        
        Long memberId = jwtMemberExtractor.extractMemberId(httpRequest);
        return dailyRecordFacade.getDailyRecord(memberId, date);
    }
    
    @Operation(
        summary = "오늘 기록 조회",
        description = "오늘 날짜의 루틴 수행 기록과 회고를 조회합니다. (편의 API)"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/today")
    @PreAuthorize("hasRole('USER')")
    public DailyRecordResponse getTodayRecord(HttpServletRequest httpRequest) {
        
        Long memberId = jwtMemberExtractor.extractMemberId(httpRequest);
        return dailyRecordFacade.getDailyRecord(memberId, LocalDate.now());
    }
}