package com.example.demo.payment.domain;

/** PG 승인 실패. {@code reason} 은 우리 쪽 분류다(PG 의 오류 코드를 그대로 쓰지 않는다). */
public class PaymentGatewayException extends RuntimeException {

    public enum Reason {
        EMPTY_RESPONSE,
        PG_REJECTED,
        NOT_APPROVED,
        ORDER_MISMATCH,
        AMOUNT_MISMATCH,
        MISSING_APPROVED_AT
    }

    private final Reason reason;

    public PaymentGatewayException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
