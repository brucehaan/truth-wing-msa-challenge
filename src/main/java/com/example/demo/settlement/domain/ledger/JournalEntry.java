package com.example.demo.settlement.domain.ledger;

import com.example.demo.settlement.domain.ledger.exception.UnbalancedJournalException;
import com.example.demo.settlement.domain.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 전표 - 원장의 Aggregate Root
 * 한 번 만들어지면 바뀌지 않는다. 모든 분개의 합은 0이다.
 * setter도, 분개를 추가하는 메서드도 없다. 정정은 reversal() (역분개)로 새 전표를 만든다.
 */
public final class JournalEntry {
    private final UUID id;
    private final JournalHeader header;
    private final List<Posting> postings;

    private JournalEntry(UUID id, JournalHeader header, List<Posting> postings) {
        this.id = id;
        this.header = header;
        this.postings = List.copyOf(postings); // 방어적 복사 + 불변
    }

    public static JournalEntry post(JournalHeader header, List<Posting> postings) {
        validate(header, postings);
        return new JournalEntry(UUID.randomUUID(), header, postings);
    }

    /**
     * 영속성 어댑터가 DB행을 도메인으로 되돌릴 때 쓴다.
     * 읽을 때도 차대 균형을 다시 검증한다 - DB에서 누군가 행을 고쳤다면 여기서 드러난다.
     */
    public static JournalEntry restore(UUID id, JournalHeader header, List<Posting> postings) {
        validate(header, postings);
        return new JournalEntry(id, header, postings);
    }

    private static void validate(JournalHeader header, List<Posting> postings) {
        if (postings == null || postings.size() < 2) {
            throw new IllegalArgumentException("전표는 분개가 2줄 이상이어야 합니다.");
        }
        Money sum = postings.stream().map(Posting::amount).reduce(Money.ZERO, Money::plus);
        if (!sum.isZero()) {
            throw new UnbalancedJournalException(header.sourceKey(), sum);
        }
    }

    public JournalEntry reversal(LocalDate businessDate, Instant at, String issuedBy) {
        JournalHeader reversalHeader = new JournalHeader(
                JournalType.REVERSAL,
                SourceKey.reversalOf(id), // 같은 전표를 두 번 정정하면 멱등 키 충돌
                header.orderNo(),
                header.sellerId(),
                businessDate,
                at,
                header.feePolicyId(),
                id,
                issuedBy
        );
        return post(reversalHeader, postings.stream().map(Posting::negate).toList());
    }

    /* 이 전표에서 특정 계정에 기록된 부호 있는 합계 */
    public Money sumFor(AccountCode account) {
        return postings.stream()
                .filter(p -> p.account().equals(account))
                .map(Posting::amount)
                .reduce(Money.ZERO, Money::plus);
    }

    public UUID id() { return id; }
    public JournalHeader header() { return header; }
    public List<Posting> postings() { return postings; }
}
