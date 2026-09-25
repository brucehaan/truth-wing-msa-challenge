package com.example.demo.settlement.domain.ledger;

import com.example.demo.settlement.domain.fee.FeePolicy;
import com.example.demo.settlement.domain.money.Money;

/**
 * 환불 분개에 필요한 원거래 정보. 원장에서 계산해 넘긴다.
 * remaining* 는 "원 판매 - 지금까지의 환불" 이다. 부분 환불이 여러 번 일어나도 수수료가 정확히 환입되게 한다.
 */
public record RefundContext(
        String sellerId,
        FeePolicy originalPolicy,
        Money remainingGross,
        Money remainingCommission
) {
}
