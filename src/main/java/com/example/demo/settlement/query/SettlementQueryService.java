package com.example.demo.settlement.query;

import com.example.demo.settlement.query.view.SellerSummaryView;
import com.example.demo.settlement.query.view.SettlementDayView;
import com.example.demo.settlement.query.view.StatementView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정산 조회 서비스(CQRS — Query). 조회 모델만 읽는다. 원장을 집계하지 않으므로 조회량이 늘어도 마감·수집과 경합하지 않는다.
 * 대가: 마감 직후 릴레이가 이벤트를 전달하기 전까지(기본 1초 주기) 조회 결과가 뒤처질 수 있다 — 최종적 일관성.
 */
@Service
@Transactional(readOnly = true)
public class SettlementQueryService {

    private static final int MAX_RANGE_DAYS = 366;

    private final SettlementReadModel readModel;

    public SettlementQueryService(SettlementReadModel readModel) {
        this.readModel = readModel;
    }

    public List<StatementView> statements(String sellerId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from 은 to 보다 늦을 수 없습니다: " + from + " > " + to);
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new IllegalArgumentException("조회 기간은 최대 " + MAX_RANGE_DAYS + "일입니다");
        }
        return readModel.statementsOf(sellerId, from, to);
    }

    public Optional<SellerSummaryView> summary(String sellerId) {
        return readModel.summaryOf(sellerId);
    }

    public Optional<SettlementDayView> day(LocalDate businessDate) {
        return readModel.day(businessDate);
    }
}
