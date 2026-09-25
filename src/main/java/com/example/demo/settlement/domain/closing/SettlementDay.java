package com.example.demo.settlement.domain.closing;

import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * 정산일 - 마감의 단위이자 "마감해도 되는가"를 판정하는 Aggregate.
 * 마감 조건 세 가지를 이 객체가 강제한다
 *    1) 필수 원천이 모두 완결성 검증을 통과했다 (통제 합계 일치)
 *    2) 그날 원장의 시산표 합계가 0이다 (복식부기 무결성)
 *    3) 아직 마감되지 않았다
 */
public final class SettlementDay {
    private final LocalDate date;
    private DayStatus status;
    private final Map<String, ControlTotal> verifiedSources;
    private Instant closedAt;

    private SettlementDay(LocalDate date, DayStatus status, Map<String, ControlTotal> verifiedSources, Instant closedAt) {
        this.date = Objects.requireNonNull(date, "date");
        this.status = status;
        this.verifiedSources = new HashMap<>(verifiedSources);
        this.closedAt = closedAt;
    }

    /* 영속성 어댑터가 복원할 때 쓰는 팩토리 */
    public static SettlementDay restore(LocalDate date, DayStatus status, Map<String, ControlTotal> verified, Instant closedAt) {
        return new SettlementDay(date, status, verified, closedAt);
    }

    public void recordVerifiedSource(String source, ControlTotal total) {
        verifiedSources.put(source, total); // 마감 뒤 재검증 기록도 허용 (감사용). 마감 상태는 되돌리지 않는다.
    }

    public void close(Set<String> requiredSources, Money trialBalance, Instant now) {
        if (status == DayStatus.CLOSED) {
            throw new IllegalStateException("이미 마감된 정산일입니다: " + date);
        }
        Set<String> missing = new HashSet<>(requiredSources);
        missing.removeAll(verifiedSources.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("완결성 검증을 통과하지 않은 원천이 있습니다: " + missing + ", date=" + date);
        }
        if (!trialBalance.isZero()) {
            throw new IllegalStateException("시산표가 맞지 않습니다. 차대 합계=" + trialBalance + ", date=" + date);
        }
        this.status = DayStatus.CLOSED;
        this.closedAt = now;
    }

    public boolean isClosed() { return status == DayStatus.CLOSED; }
    public LocalDate date() { return date; }
    public DayStatus status() { return status; }
    public Instant closedAt() { return closedAt; }
    public Map<String, ControlTotal> verifiedSources() { return Map.copyOf(verifiedSources); }
}
