package com.example.demo.order.application.port.in;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문이 정산에 공개하는 조회 포트 — Open Host Service.
 * 정산은 주문의 엔티티·리포지토리가 아니라 이 인터페이스와 두 레코드(OrderSettlementFact, OrderControlTotal)만 안다.
 * 번역은 정산 쪽 ACL(settlement.adapter.out.order)이 한다.
 */
public interface OrderSettlementQuery {

    /**
     * D일(paidAt 기준)에 결제된 주문 사실. "정산 대상인가"는 판단하지 않는다.
     * 현재 status 로 거르지 않는다 — D일 결제 후 취소된 주문도 "D일에 결제됐다"는 사실은 남고, 취소는 환불 사실로 상쇄된다.
     */
    List<OrderSettlementFact> exportPaidFacts(LocalDate paidDate);

    /** 목록과 별도 쿼리로 센 D일 통제 합계 — 적재 경로와 독립이어야 대조가 의미를 갖는다. */
    OrderControlTotal paidControlTotal(LocalDate paidDate);
}
