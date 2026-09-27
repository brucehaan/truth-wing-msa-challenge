package com.example.demo.settlement.adapter.out.memory;

import com.example.demo.settlement.application.port.out.LedgerPort;
import com.example.demo.settlement.domain.ledger.AccountCode;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.Posting;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** 테스트용 원장 어댑터. JPA 어댑터와 같은 계약(append-only, sourceKey 유일)을 지킨다. */
public class InMemoryLedger implements LedgerPort {

    private final Map<SourceKey, JournalEntry> entries = new LinkedHashMap<>();
    private final List<RawPosting> tampered = new ArrayList<>();

    @Override
    public boolean exists(SourceKey key) {
        return entries.containsKey(key);
    }

    @Override
    public boolean appendIfAbsent(JournalEntry entry) {
        return entries.putIfAbsent(entry.header().sourceKey(), entry) == null;
    }

    @Override
    public Optional<JournalEntry> findBySourceKey(SourceKey key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public List<JournalEntry> findByOrderNo(String orderNo) {
        return entries.values().stream().filter(e -> Objects.equals(orderNo, e.header().orderNo())).toList();
    }

    @Override
    public Money sumPostings(AccountCode account, LocalDate businessDate) {
        return entries.values().stream()
                .filter(e -> e.header().businessDate().equals(businessDate))
                .map(e -> e.sumFor(account))
                .reduce(Money.ZERO, Money::plus);
    }

    @Override
    public Money trialBalance(LocalDate businessDate) {
        Money fromEntries = entries.values().stream()
                .filter(e -> e.header().businessDate().equals(businessDate))
                .flatMap(e -> e.postings().stream())
                .map(Posting::amount)
                .reduce(Money.ZERO, Money::plus);
        Money fromTampered = tampered.stream()
                .filter(t -> t.date().equals(businessDate))
                .map(RawPosting::amount)
                .reduce(Money.ZERO, Money::plus);
        return fromEntries.plus(fromTampered);
    }

    @Override
    public Set<String> sellersWithPostingsOn(LocalDate businessDate) {
        Set<String> sellers = new TreeSet<>();
        entries.values().stream()
                .filter(e -> e.header().businessDate().equals(businessDate))
                .flatMap(e -> e.postings().stream())
                .filter(p -> p.account().kind().isPerSeller())
                .forEach(p -> sellers.add(p.account().ownerId()));
        return sellers;
    }

    public int size() {
        return entries.size();
    }

    /** DB 에 직접 INSERT 한 것처럼 도메인을 우회해 분개 한 줄을 넣는다 — 시산표 검증 테스트용. */
    public void tamperDirectly(LocalDate date, Money amount) {
        tampered.add(new RawPosting(date, amount));
    }

    private record RawPosting(LocalDate date, Money amount) {}
}
