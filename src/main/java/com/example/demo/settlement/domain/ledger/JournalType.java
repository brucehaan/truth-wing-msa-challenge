package com.example.demo.settlement.domain.ledger;

public enum JournalType {
    SALE, // 판매
    REFUND, // 환불
    REVERSAL, // 정정 (원 전표 전체를 반대 부호로)
    ADJUSTMENT, // 수기 조정
    PG_SETTLEMENT, // PG 입금 (대사 단계)
    PAYOUT // 판매자 지급 (지급 단계)
}
