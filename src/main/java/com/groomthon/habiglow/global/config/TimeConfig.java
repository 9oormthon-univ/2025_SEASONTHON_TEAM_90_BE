package com.groomthon.habiglow.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TimeConfig {
    
    @Bean
    @Primary
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}