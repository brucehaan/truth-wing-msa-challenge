package com.example.demo.order.application.port.in;

import java.math.BigDecimal;

/** 주문 DB 에서 직접 센 D일 통제 합계 — 목록(exportPaidFacts)과 독립된 쿼리로 만들어야 대조가 의미를 갖는다. */
public record OrderControlTotal(long count, BigDecimal sum) {
}
