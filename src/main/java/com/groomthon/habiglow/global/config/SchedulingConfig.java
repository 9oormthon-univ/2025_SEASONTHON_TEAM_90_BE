package com.groomthon.habiglow.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

//주기적 작업을 자동 실행하도록 설정
@Configuration
@EnableScheduling
public class SchedulingConfig {}
