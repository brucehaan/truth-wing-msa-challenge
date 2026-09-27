package com.example.demo.order.published;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 컨텍스트가 공개하는 "결제된 주문" 한 건 — Published Language.
 * 주문의 언어 그대로다(판매자 UUID, BigDecimal 금액, Asia/Seoul 기준 LocalDateTime).
 * 정산의 언어로 바꾸는 일은 정산 쪽 ACL 의 책임이다.
 */
public record PaidOrderRow(String orderNo, UUID sellerId, BigDecimal grossAmount, LocalDateTime paidAt) {
}
