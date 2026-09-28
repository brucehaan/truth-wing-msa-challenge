package com.example.demo.settlement.adapter.out.order;

import com.example.demo.order.application.port.in.OrderControlTotal;
import com.example.demo.order.application.port.in.OrderSettlementFact;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.money.Money;

import java.math.BigDecimal;

/**
 * ACL 의 번역기 — 주문의 언어를 정산의 언어로 바꾼다. 스프링에 의존하지 않는 순수 클래스다.
 *   판매자 UUID → 문자열 / BigDecimal(15,2) → 원 단위 Money(10000.00 허용, 10000.50 거부) / KST LocalDateTime → Instant
 */
public final class OrderFactTranslator {

    public SaleFact toSaleFact(OrderSettlementFact fact) {
        if (fact.orderNo() == null || fact.orderNo().isBlank()) {
            throw new UntranslatableFactException("?", "주문번호가 없습니다");
        }
        if (fact.sellerId() == null) {
            throw new UntranslatableFactException(fact.orderNo(), "판매자가 없습니다");
        }
        if (fact.paidAt() == null) {
            throw new UntranslatableFactException(fact.orderNo(), "결제 시각이 없습니다");
        }
        return new SaleFact(
                fact.orderNo(),
                null,                                            // 주문 모델에 paymentKey 가 없다 — 대사 단계에서 결제와 잇는다
                fact.sellerId().toString(),
                won(fact.grossAmount(), fact.orderNo()),
                fact.paidAt().atZone(BusinessCalendar.ZONE).toInstant());
    }

    public ControlTotal toControlTotal(OrderControlTotal totals) {
        if (totals.count() < 0) {
            throw new UntranslatableFactException("control-total", "건수가 음수입니다: " + totals.count());
        }
        return new ControlTotal(totals.count(), won(totals.sum(), "control-total"));
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
