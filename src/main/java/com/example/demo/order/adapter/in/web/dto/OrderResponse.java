package com.example.demo.order.adapter.in.web.dto;

import com.example.demo.order.domain.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNo,
        UUID buyerId,
        UUID sellerId,
        UUID productId,
        Long quantity,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        String status,
        LocalDateTime paidAt,
        Boolean settled,
        UUID settlementBatchId,
        UUID regId,
        LocalDateTime regDt,
        UUID modifyId,
        LocalDateTime modifyDt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getGrossAmount(),
                order.getFeeAmount(),
                order.getRefundAmount(),
                order.getNetAmount(),
                order.getStatus(),
                order.getPaidAt(),
                order.getSettled(),
                order.getSettlementBatchId(),
                order.getRegId(),
                order.getRegDt(),
                order.getModifyId(),
                order.getModifyDt()
        );
    }
}
