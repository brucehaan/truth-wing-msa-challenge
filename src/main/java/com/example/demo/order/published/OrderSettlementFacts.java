package com.example.demo.order.published;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 컨텍스트가 정산에 제공하는 조회 API — Open Host Service.
 * 정산은 주문의 엔티티·리포지토리를 보지 않고 이 인터페이스와 {@code published} 패키지의 타입만 쓴다.
 *
 * <p>규칙</p>
 * <ul>
 *   <li>기간은 반열림 [from, to) 이고 paidAt 기준이다</li>
 *   <li>현재 status 로 거르지 않는다 — 결제 후 취소된 주문도 "그날 결제됐다"는 사실은 남아야 한다(취소는 환불 사실로 따로 들어온다)</li>
 *   <li>0원 주문은 정산 사실이 아니므로 목록과 통제 합계 양쪽에서 똑같이 뺀다</li>
 * </ul>
 */
public interface OrderSettlementFacts {

    List<PaidOrderRow> findPaidBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    PaidOrderTotals totalsPaidBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
