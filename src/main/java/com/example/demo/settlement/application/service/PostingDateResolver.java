package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import com.example.demo.settlement.domain.closing.SettlementDay;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 전표를 어느 정산일에 반영할지 정한다.
 * 원래 날짜가 아직 열려 있으면 그 날짜, 이미 마감됐으면 오늘로 이월한다.
 * 마감된 날짜의 금액은 절대 바뀌지 않는다(I2) — 이 규칙 하나가 그것을 보장한다.
 */
class PostingDateResolver {

    private final SettlementDayPort dayPort;
    private final Clock clock;

    PostingDateResolver(SettlementDayPort dayPort, Clock clock) {
        this.dayPort = dayPort;
        this.clock = clock;
    }

    LocalDate resolve(LocalDate preferred) {
        boolean preferredClosed = dayPort.find(preferred).map(SettlementDay::isClosed).orElse(false);
        if (!preferredClosed) {
            return preferred;
        }
        LocalDate today = today();
        if (dayPort.find(today).map(SettlementDay::isClosed).orElse(false)) {
            throw new IllegalStateException("오늘 정산일까지 마감되어 반영할 곳이 없습니다: " + today);
        }
        return today;
    }

    LocalDate today() {
        return BusinessCalendar.dateOf(clock.instant());
    }
}
