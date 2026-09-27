package com.example.demo.settlement.query.view;

import java.time.LocalDate;

/** 판매자 누적 요약. 정산서 조회 모델에서 매번 다시 계산하므로 이벤트가 두 번 와도 값이 같다. */
public record SellerSummaryView(String sellerId, long totalPayable, int statementCount, LocalDate lastBusinessDate) {
}
