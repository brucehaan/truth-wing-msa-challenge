package com.example.demo.settlement.adapter.out.order;

import com.example.demo.order.published.PaidOrderRow;
import com.example.demo.order.published.PaidOrderTotals;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.money.Money;

import java.math.BigDecimal;

/**
 * ACL 의 번역기 — 주문의 언어를 정산의 언어로 바꾼다. 스프링에 의존하지 않는 순수 클래스다.
 *
 * <table>
 *   <caption>번역 규칙</caption>
 *   <tr><th>주문(Published Language)</th><th>정산(도메인)</th><th>이유</th></tr>
 *   <tr><td>sellerId: UUID</td><td>String</td><td>정산 계정의 소유자 키는 문자열 — 판매자 식별 체계가 바뀌어도 원장 스키마는 그대로</td></tr>
 *   <tr><td>grossAmount: BigDecimal(15,2)</td><td>Money(long 원)</td><td>KRW 는 소수 자릿수 0. 10000.00 은 허용, 10000.50 은 거부</td></tr>
 *   <tr><td>paidAt: LocalDateTime(Asia/Seoul)</td><td>occurredAt: Instant</td><td>시각은 절대 시점으로, 날짜는 영업 타임존으로 자를 때만 만든다</td></tr>
 * </table>
 */
public final class OrderFactTranslator {

    public SaleFact toSaleFact(PaidOrderRow row) {
        if (row.orderNo() == null || row.orderNo().isBlank()) {
            throw new UntranslatableFactException("?", "주문번호가 없습니다");
        }
        if (row.sellerId() == null) {
            throw new UntranslatableFactException(row.orderNo(), "판매자가 없습니다");
        }
        if (row.paidAt() == null) {
            throw new UntranslatableFactException(row.orderNo(), "결제 시각이 없습니다");
        }
        return new SaleFact(
                row.orderNo(),
                null,                                            // 주문 모델에 paymentKey 가 없다 — 대사 단계에서 결제와 잇는다
                row.sellerId().toString(),
                won(row.grossAmount(), row.orderNo()),
                row.paidAt().atZone(BusinessCalendar.ZONE).toInstant());
    }

    public ControlTotal toControlTotal(PaidOrderTotals totals) {
        if (totals.count() < 0) {
            throw new UntranslatableFactException("control-total", "건수가 음수입니다: " + totals.count());
        }
        return new ControlTotal(totals.count(), won(totals.grossSum(), "control-total"));
    }

    static Money won(BigDecimal amount, String reference) {
        if (amount == null) {
            throw new UntranslatableFactException(reference, "금액이 없습니다");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new UntranslatableFactException(reference, "원 단위가 아닌 금액입니다: " + amount.toPlainString());
        }
        return Money.won(normalized.longValueExact());
    }
}
