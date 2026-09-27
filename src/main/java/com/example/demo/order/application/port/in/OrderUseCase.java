package com.example.demo.order.application.port.in;

import com.example.demo.order.adapter.in.web.dto.OrderCreateRequest;
import com.example.demo.order.adapter.in.web.dto.OrderResponse;

import java.time.LocalDate;
import java.util.List;

public interface OrderUseCase {
    OrderResponse create(OrderCreateRequest request);

    List<OrderResponse> getAll();

    List<OrderResponse> getSettlementCandidates(LocalDate settlementDate);

    /**
     * D일(paidAt 기준)에 결제된 주문 사실 — 정산이 가져간다 (README Step 9). "정산 대상인가"는 판단하지 않는다.
     * 현재 status 로 거르지 않는다: D일 결제 후 취소된 주문도 "D일에 결제됐다"는 사실은 남아야 판매 전표가 생기고,
     * 취소는 별도의 환불 사실로 들어와 상쇄된다.
     */
    List<OrderSettlementFact> exportPaidFacts(LocalDate paidDate);

    /** 주문 DB 에서 목록과 별도 쿼리로 센 D일 통제 합계 — 적재 경로와 독립이어야 대조가 의미를 갖는다. */
    OrderControlTotal paidControlTotal(LocalDate paidDate);
}
