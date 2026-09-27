package com.example.demo.order.published;

import java.math.BigDecimal;

/** 주문 DB 에서 직접 센 통제 합계. 목록 조회와 독립된 집계 쿼리로 만들어야 대조가 의미를 갖는다. */
public record PaidOrderTotals(long count, BigDecimal grossSum) {
}
