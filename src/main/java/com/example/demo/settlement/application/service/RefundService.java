package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.RefundUseCase;
import com.example.demo.settlement.application.port.out.FeePolicyPort;
import com.example.demo.settlement.application.port.out.LedgerPort;
import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import com.example.demo.settlement.domain.fee.FeePolicy;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.*;
import com.example.demo.settlement.domain.money.Money;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 환불 유스케이스 - 원 판매 전표는 건드리지 않고 반대 방향 전표를 추가한다
 */
public class RefundService implements RefundUseCase {

    private static final Set<JournalType> SALES_LIFECYCLE = EnumSet.of(JournalType.SALE, JournalType.REFUND, JournalType.REVERSAL);

    private final LedgerPort ledgerPort;
    private final FeePolicyPort feePolicyPort;
    private final JournalFactory journalFactory = new JournalFactory();
    private final PostingDateResolver postingDateResolver;

    public RefundService(LedgerPort ledgerPort, FeePolicyPort feePolicyPort, SettlementDayPort dayPort, Clock clock) {
        this.ledgerPort = ledgerPort;
        this.feePolicyPort = feePolicyPort;
        this.postingDateResolver = new PostingDateResolver(dayPort, clock);
    }

    @Override
    public JournalEntry refund(RefundFact fact) {
        SourceKey key = SourceKey.refund(fact.refundId());
        var existing = ledgerPort.findBySourceKey(key);
        if (existing.isPresent()) return existing.get();
        List<JournalEntry> lifeCycle = ledgerPort.findByOrderNo(fact.orderNo()).stream()
                .filter(e -> SALES_LIFECYCLE.contains(e.header().type()))
                .toList();

        JournalEntry sale = lifeCycle.stream()
                .filter(e -> e.header().type() == JournalType.SALE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("원 판매 전표가 없습니다. orderNo=" + fact.orderNo()));

        AccountCode pgReceivable = AccountCode.of(AccountKind.PG_RECEIVABLE);
        AccountCode commissionRevenue = AccountCode.of(AccountKind.COMMISSION_REVENUE);

        // 정상 잔액 방향으로 읽으면 판매 +, 환불 및 정정 -가 자연스럽게 합산된다
        Money remainingGross = AccountKind.PG_RECEIVABLE.type().normalBalance(sumOf(lifeCycle, pgReceivable));
        Money remainingCommission = AccountKind.COMMISSION_REVENUE.type().normalBalance(sumOf(lifeCycle, commissionRevenue));

        FeePolicy originalPolicy = feePolicyPort.getById(sale.header().feePolicyId());
        RefundContext ctx = new RefundContext(sale.header().sellerId(), originalPolicy, remainingGross, remainingCommission);

        LocalDate postingDate = postingDateResolver.resolve(BusinessCalendar.dateOf(fact.occurredAt()));
        JournalEntry refund = journalFactory.refund(fact, ctx, postingDate);
        if (ledgerPort.appendIfAbsent(refund)) return refund;
        return ledgerPort.findBySourceKey(key).orElseThrow(); // 동시 수신하면 먼저 들어간 쪽을 돌려준다
    }

    private static Money sumOf(List<JournalEntry> entries, AccountCode account) {
        return entries.stream().map(e -> e.sumFor(account)).reduce(Money.ZERO, Money::plus);
    }
}
