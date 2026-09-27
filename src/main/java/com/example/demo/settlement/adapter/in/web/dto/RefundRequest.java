package com.example.demo.settlement.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "환불 사실 등록 요청 — 부분 환불은 건마다 다른 refundId")
public record RefundRequest(
        @Schema(description = "환불 식별자(멱등 키)", example = "RF-0001")
        String refundId,
        @Schema(description = "원 주문번호", example = "ORD-20260115100000-ABCDEF12")
        String orderNo,
        @Schema(description = "환불 금액(원)", example = "3000")
        long amount,
        @Schema(description = "환불 발생 시각(ISO-8601). 없으면 요청 시각", example = "2026-01-15T13:00:00+09:00")
        Instant occurredAt
) {
}
