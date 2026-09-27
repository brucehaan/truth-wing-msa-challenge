package com.example.demo.settlement.query.view;

import java.time.Instant;
import java.time.LocalDate;

/** 판매자 × 정산일 정산서 조회 모델. payable 이 음수면 다음 지급으로 이월된다. */
public record StatementView(String sellerId, LocalDate businessDate, long payable, Instant closedAt) {
}
