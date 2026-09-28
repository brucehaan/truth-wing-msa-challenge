package com.example.demo.payment.published;

import com.example.demo.common.event.IntegrationEvent;
import com.example.demo.payment.domain.PaymentApproval;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 결제가 승인됐다 — 결제 컨텍스트가 공개하는 사실(Published Language).
 * 주문 컨텍스트가 이 이벤트를 받아 주문을 결제 완료로 확정한다(1주차 ADR-022 의 동기 호출을 이벤트로 대체).
 *
 * @param method     결제 도메인의 결제수단 이름({@code PaymentMethod} 의 name). PG 표기가 아니다
 * @param approvedAt PG 승인 시각 — 주문의 paidAt 이 된다
 */
public record PaymentApproved(
        UUID eventId,
        Instant occurredAt,
        String paymentKey,
        String orderNo,
        long amount,
        String method,
        Instant approvedAt
) implements IntegrationEvent {

    public PaymentApproved {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(paymentKey, "paymentKey");
        Objects.requireNonNull(orderNo, "orderNo");
        Objects.requireNonNull(approvedAt, "approvedAt");
    }

    public static PaymentApproved of(PaymentApproval approval) {
        return new PaymentApproved(UUID.randomUUID(), approval.approvedAt(), approval.paymentKey(),
                approval.orderNo(), approval.amount(), approval.method().name(), approval.approvedAt());
    }

    @Override
    public String aggregateKey() {
        return orderNo;
    }
}
