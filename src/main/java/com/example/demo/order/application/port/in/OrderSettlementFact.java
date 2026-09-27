package com.example.demo.order.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문이 정산에 공개하는 사실의 모양 (Published Language, README Step 9) — 주문 모듈이 소유한다.
 * 정산은 이 타입만 알고 Order 엔티티는 모른다. 주문의 언어 그대로다(UUID 판매자, BigDecimal 금액, Asia/Seoul 기준 LocalDateTime).
 */
public record OrderSettlementFact(String orderNo, UUID sellerId, BigDecimal grossAmount, LocalDateTime paidAt) {
}
