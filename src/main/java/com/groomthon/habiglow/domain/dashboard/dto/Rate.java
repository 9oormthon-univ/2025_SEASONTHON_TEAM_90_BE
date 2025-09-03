package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "완료/전체/비율")
public class Rate {
    private int done;
    private int total;
    private double rate; // 0~100
}
