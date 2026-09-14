package com.example.demo.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "\"payment_failure\"", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentFailure {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "amount")
    private Long amount;

    /*
    PostgreSQL에서 @Lob + String 은 large object(OID)로 매핑돼 조회 시 깨지는 경우가 있어,
    columnDefinition = "TEXT"만 사용한다. (demo 의 Product.description과 동일한 방식)
     */
    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private PaymentFailure(
            String orderId,
            String paymentKey,
            String errorCode,
            String errorMessage,
            Long amount,
            String rawPayload
    ) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.amount = amount;
        this.rawPayload = rawPayload;
    }

    public static PaymentFailure create(
            String orderId,
            String paymentKey,
            String errorCode,
            String errorMessage,
            Long amount,
            String rawPayload
    ) {
        return new PaymentFailure(orderId, paymentKey, errorCode, errorMessage, amount, rawPayload);
    }

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
    }
}
