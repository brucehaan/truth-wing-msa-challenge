package com.example.demo.settlement.adapter.out.order;

import com.example.demo.order.application.port.in.OrderSettlementQuery;
import com.example.demo.settlement.application.port.out.SaleSourcePort;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 → 정산 Anti-Corruption Layer. 정산이 자기 언어로 정의한 SaleSourcePort 를, 주문의 공개 포트(OrderSettlementQuery)로 구현한다.
 * 정산에서 주문을 아는 곳은 이 패키지뿐이다(아키텍처 테스트로 강제). 서비스를 나누면 이 클래스 내부만 gRPC 호출로 바뀐다.
 */
@Component
public class OrderSaleSourceAdapter implements SaleSourcePort {

    private final OrderSettlementQuery orders;
    private final OrderFactTranslator translator = new OrderFactTranslator();

    public OrderSaleSourceAdapter(OrderSettlementQuery orders) {
        this.orders = orders;
    }

    @Override
    public List<SaleFact> paidOn(LocalDate occurredDate) {
        return orders.exportPaidFacts(occurredDate).stream().map(translator::toSaleFact).toList();
    }

    @Override
    public ControlTotal declaredTotal(LocalDate occurredDate) {
        return translator.toControlTotal(orders.paidControlTotal(occurredDate));
    }
}
