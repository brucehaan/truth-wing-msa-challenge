package com.example.demo.order.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreateRequest(
        String orderNo,
        UUID buyerId,
        UUID sellerId,
        UUID productId,
        Long quantity,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal refundAmount,
        String status,
        LocalDateTime paidAt,
        UUID actorId
) {
}
