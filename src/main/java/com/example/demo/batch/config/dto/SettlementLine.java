package com.example.demo.batch.config.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 청크 스텝에서 ItemProcessor가 만들어 ItemWriter로 넘기는 정산 라인
 * 기존에는 SettlementBatchConfig의 중첩 record였는데, demo의 dto 패키지 규칙에 맞춰 분리했다.
 */
public record SettlementLine(
        UUID orderId,
        String orderNo,
        UUID sellerId,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        LocalDateTime paidAt
) {
}
