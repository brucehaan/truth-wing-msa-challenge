package com.example.demo.settlement.domain.closing;

import com.example.demo.settlement.domain.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 마감 결과 - 판매자 * 정산일의 확정 지급 대상액. 마감 시 한 번 만들어지고 바뀌지 않는다.
 * payable이 음수면(환불 > 판매) 지급하지 않고 다음으로 이월한다 - 지급 단계의 책임.
 */
public record SellerStatement(
        String sellerId,
        LocalDate businessDate,
        Money payable,
        Instant closedAt
) {
}
