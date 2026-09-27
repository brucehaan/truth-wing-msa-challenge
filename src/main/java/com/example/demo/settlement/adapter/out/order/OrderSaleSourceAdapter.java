package com.example.demo.settlement.adapter.out.order;

import com.example.demo.order.published.OrderSettlementFacts;
import com.example.demo.settlement.application.port.out.SaleSourcePort;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 → 정산 Anti-Corruption Layer.
 *
 * <p>정산에서 주문 모듈을 import 하는 곳은 이 패키지(adapter.out.order)뿐이다(아키텍처 테스트로 강제).
 * 그마저도 주문의 엔티티·리포지토리가 아니라 주문이 공개한 {@code order.published} 만 쓴다.
 * 서비스를 분리하면 이 클래스의 내부만 gRPC 클라이언트 호출로 바뀌고, 정산의 포트·도메인은 그대로다.</p>
 */
@Component
public class OrderSaleSourceAdapter implements SaleSourcePort {

    private final OrderSettlementFacts orders;
    private final OrderFactTranslator translator = new OrderFactTranslator();

    public OrderSaleSourceAdapter(OrderSettlementFacts orders) {
        this.orders = orders;
    }

    @Override
    public List<SaleFact> paidOn(LocalDate occurredDate) {
        return orders.findPaidBetween(startOf(occurredDate), startOf(occurredDate.plusDays(1))).stream()
                .map(translator::toSaleFact)
                .toList();
    }

    @Override
    public ControlTotal declaredTotal(LocalDate occurredDate) {
        return translator.toControlTotal(
                orders.totalsPaidBetween(startOf(occurredDate), startOf(occurredDate.plusDays(1))));
    }

    /** 주문의 paidAt 은 Asia/Seoul 기준 LocalDateTime 이므로, 영업일 D 는 [D 00:00, D+1 00:00) 로 자른다. */
    private static LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }
}
