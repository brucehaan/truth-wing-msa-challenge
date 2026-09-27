package com.example.demo.settlement.adapter.out.memory;

import com.example.demo.settlement.application.port.out.OutboxPort;
import com.example.demo.settlement.application.port.out.SellerStatementPort;
import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.closing.SellerStatement;
import com.example.demo.settlement.domain.closing.SettlementDay;

import java.time.LocalDate;
import java.util.*;

/**
 * 정산일, 정산서, 아웃박스를 한 곳에 둔 테스트 어댑터
 */
public class InMemoryClosing implements SettlementDayPort, SellerStatementPort, OutboxPort {

    private final Map<LocalDate, SettlementDay> days = new HashMap<>();
    private final Map<String, SellerStatement> statements = new HashMap<>();
    public final List<String> outbox = new ArrayList<>();

    @Override
    public Optional<SettlementDay> find(LocalDate date) {
        return Optional.ofNullable(days.get(date));
    }

    @Override
    public Optional<SellerStatement> find(String sellerId, LocalDate date) {
        return Optional.ofNullable(statements.get(sellerId + "|" + date));
    }

    @Override
    public void save(SellerStatement sellerStatement) {
        if (statements.putIfAbsent(sellerStatement.sellerId() + "|" + sellerStatement.businessDate(), sellerStatement) != null) {
            throw new IllegalStateException("정산서는 한 번만 발행됩니다");
        }
    }

    @Override
    public List<SellerStatement> findByDate(LocalDate date) {
        return statements.values().stream().filter(s -> s.businessDate().equals(date)).toList();
    }

    @Override
    public void save(SettlementDay day) {
        days.put(day.date(), day);
    }

    @Override
    public void append(String eventType, String aggregateKey, String payload) {
        outbox.add(eventType + "|" + aggregateKey);
    }
}
