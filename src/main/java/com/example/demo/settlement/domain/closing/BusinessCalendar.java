package com.example.demo.settlement.domain.closing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 정산의 날짜는 서버 타임존이 아니라 영업 기준 타임존으로 자른다.
 * UTC서버에서 9시간 밀리는 것을 막는다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessCalendar {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static LocalDate dateOf(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }
}
