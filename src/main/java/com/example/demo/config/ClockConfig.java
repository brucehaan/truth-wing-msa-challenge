package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 애플리케이션 전체가 쓰는 시계. 도메인은 Instant 로 시각을 다루고, 날짜로 자를 때만 Asia/Seoul 을 명시한다
 * (settlement.domain.closing.BusinessCalendar). 테스트에서는 고정 시계로 바꿔 끼운다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
