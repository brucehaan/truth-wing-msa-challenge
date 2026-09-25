package com.example.demo.settlement.domain.ledger;

import com.example.demo.settlement.domain.fee.FeePolicy;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 도메인 서비스 - 분개 규칙.
 * "판매가 일어나면 어느 계정에 얼마를 적는가"가 정산 도메인의 핵심 지식이다.
 * 이 클래스는 스프링도 JPA도 모른다. 그래서 규칙을 DB 없이 전수 테스트할 수 있다.
 */
public final class JournalFactory {
    private static final String SYSTEM = "SYSTEM";

    /**
     * 판매 분개.
     *   차) PG 미수금        gross
     *      대) 판매자 미지급금 gross - 수수료
     *      대) 판매수수료 수익 수수료
     */
    public JournalEntry sale(SaleFact fact, FeePolicy policy, LocalDate businessDate) {
        Money gross = fact.gross();
        Money commission = policy.commissionOf(gross);
        Money payable = gross.minus(commission);

        List<Posting> postings = new ArrayList<>();
        postings.add(Posting.debit(AccountCode.of(AccountKind.PG_RECEIVABLE), gross));
        if (payable.isPositive()) {
            postings.add(Posting.credit(AccountCode.sellerPayable(fact.sellerId()), payable));
        }
        if (commission.isPositive()) {
            postings.add(Posting.credit(AccountCode.of(AccountKind.COMMISSION_REVENUE), commission));
        }

        JournalHeader header = new JournalHeader(
                JournalType.SALE,
                SourceKey.sale(fact.orderNo()),
                fact.orderNo(),
                fact.sellerId(),
                businessDate,
                fact.occurredAt(),
                policy.id(),
                null,
                SYSTEM
        );
        return JournalEntry.post(header, postings);
    }

    /**
     * 환불 분개 - 판매의 역방향. 수수료도 환입한다.
     *    차) 판매자 미지급금  환불액 - 수수료환입
     *    차) 판매수수료 수익  수수료환입
     *      대) PG 미수금    환불액
     *
     * 부분 환불이 여러 번이면 건별 반올림 오차가 쌓인다.
     * 그래서 "잔여 금액 전부를 환불하는 건이 잔여 수수료 전부를 가져간다."
     */
    public JournalEntry refund(
            RefundFact fact,
            RefundContext ctx,
            LocalDate businessDate
    ) {
        Money amount = fact.amount();
        if (amount.compareTo(ctx.remainingGross()) > 0) {
            throw new IllegalArgumentException(
                    "환불액이 잔여 판매액을 넘습니다. 환불=" + amount + ", 잔여=" + ctx.remainingGross());
        }
        Money commissionReturn = amount.equals(ctx.remainingGross())
                ? ctx.remainingCommission()
                : ctx.originalPolicy().commissionOf(amount).minus(ctx.remainingCommission());
        Money payableReduction = amount.minus(commissionReturn);

        List<Posting> postings = new ArrayList<>();
        if (payableReduction.isPositive()) {
            postings.add(Posting.debit(AccountCode.sellerPayable(ctx.sellerId()), payableReduction));
        }
        if (commissionReturn.isPositive()) {
            postings.add(Posting.debit(AccountCode.of(AccountKind.COMMISSION_REVENUE), commissionReturn));
        }
        postings.add(Posting.credit(AccountCode.of(AccountKind.PG_RECEIVABLE), amount));

        JournalHeader header = new JournalHeader(
                JournalType.REFUND,
                SourceKey.refund(fact.refundId()),
                fact.orderNo(),
                ctx.sellerId(),
                businessDate,
                fact.occurredAt(),
                ctx.originalPolicy().id(),
                null,
                SYSTEM
        );
        return JournalEntry.post(header, postings);
    }
}
