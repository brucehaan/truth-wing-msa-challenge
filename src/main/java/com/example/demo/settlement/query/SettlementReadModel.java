package com.example.demo.settlement.query;

import com.example.demo.settlement.query.view.SellerSummaryView;
import com.example.demo.settlement.query.view.SettlementDayView;
import com.example.demo.settlement.query.view.StatementView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정산 조회 모델 저장소(CQRS 읽기 측). settlement_read 스키마만 다룬다 — 쓰기 모델(settlement 스키마)을 읽지 않는다.
 *
 * <p>갱신 메서드는 모두 멱등이고 이벤트 도착 순서에 무관해야 한다. 요약 값은 증분(+=)으로 쌓지 않고
 * 정산서 조회 모델에서 다시 계산한다 — 같은 이벤트가 두 번 와도, 마감 이벤트가 정산서 이벤트보다 먼저 와도 결과가 같다.</p>
 */
public interface SettlementReadModel {

    void upsertStatement(String sellerId, LocalDate businessDate, long payable, Instant closedAt);

    void refreshSellerSummary(String sellerId);

    /** @param closedAt 마감 이벤트에서 온 값. 정산서 이벤트에서 부를 때는 null(기존 값 유지) */
    void refreshDay(LocalDate businessDate, Instant closedAt);

    List<StatementView> statementsOf(String sellerId, LocalDate fromInclusive, LocalDate toInclusive);

    Optional<SellerSummaryView> summaryOf(String sellerId);

    Optional<SettlementDayView> day(LocalDate businessDate);
}
