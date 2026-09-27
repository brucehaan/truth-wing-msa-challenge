package com.example.demo.settlement.adapter.out.memory;

import com.example.demo.settlement.application.service.CloseService;
import com.example.demo.settlement.application.service.IntakeService;
import com.example.demo.settlement.application.service.RefundService;
import com.example.demo.settlement.application.service.ReversalService;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import com.example.demo.settlement.domain.fee.FeePolicy;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.UUID;

/**
 * 테스트마다 새로 조립하는 헥사고날 조립체 - 스프링 컨테이너가 하는 일을 손으로
 */
public class SettlementFixture {

    static final ZoneId KST = BusinessCalendar.ZONE;

    final MutableClock clock = new MutableClock(kst(2026, 1, 16 ,3, 0));
    final InMemoryLedger ledger = new InMemoryLedger();
    final InMemoryFeePolicies policies = new InMemoryFeePolicies();
    final InMemoryStaging staging = new InMemoryStaging();
    final InMemoryClosing closing = new InMemoryClosing();

    final IntakeService intake = new IntakeService(staging, staging, ledger, policies, closing, clock);
    final CloseService close = new CloseService(closing, ledger, closing, closing, clock);
    final RefundService refund = new RefundService(ledger, policies, closing, clock);
    final ReversalService reversal = new ReversalService(ledger, closing, clock);

    FeePolicy policy(String sellerId, String rate, LocalDate from, LocalDate to) {
        FeePolicy p = new FeePolicy(UUID.randomUUID(), sellerId, new BigDecimal(rate), from, to, RoundingMode.DOWN);
        policies.add(p);
        return p;
    }

    /*
    스테이징에 싣고, 상류가 선언하는 통제 합계도 함께 갱신한다.
     */
    void stageAndDeclare(LocalDate date, SaleFact... facts) {
        for (SaleFact f : facts) staging.stage(date, f);
        staging.declare(date, ControlTotal.of(staging.stagedSales(date).stream().map(SaleFact::gross).toList()));
    }

    static Instant kst(int y, int m, int d, int hh, int mm) {
        return LocalDateTime.of(y, m ,d, hh, mm).atZone(KST).toInstant();
    }

    static final class MutableClock extends Clock {

        private Instant now;
        MutableClock(Instant now) {
            this.now = now;
        }
        void setKst(LocalDateTime t) {
            now = t.atZone(KST).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return KST;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
