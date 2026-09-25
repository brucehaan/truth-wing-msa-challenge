package com.example.demo.settlement.domain.ledger;

import com.example.demo.settlement.domain.money.Money;

/**
 * 계정 유형. 차변(+)이 정상인 유형과 대변(-)이 정상인 유형이 있다.
 */
public enum AccountType {
    ASSET(true),
    EXPENSE(true),
    LIABILITY(false),
    REVENUE(false);

    private final boolean debitNormal;
    AccountType(boolean debitNormal) {
        this.debitNormal = debitNormal;
    }

    /* 부호 있는 합계(차변 +, 대변 -)를 이 유형의 정상 잔액 방향으로 읽는다. */
    public Money normalBalance(Money signedSum) {
        return debitNormal ? signedSum : signedSum.negate();
    }
}
