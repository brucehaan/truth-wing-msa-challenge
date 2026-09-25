package com.example.demo.settlement.domain.ledger;

import com.example.demo.settlement.domain.money.Money;

import java.util.Objects;

/**
 * 분개 한 줄. 금액은 부호로 방향을 표현한다.
 * 차번 +, 대변 -
 * @param account
 * @param amount
 */
public record Posting(
        AccountCode account,
        Money amount
) {
    public Posting {
        if (amount.isZero()) {
            throw new IllegalArgumentException("0원 분개는 만들지 않습니다. : " + account);
        }
    }

    public static Posting debit(AccountCode account, Money amount) {
        requirePositive(amount);
        return new Posting(account, amount);
    }

    public static Posting credit(AccountCode account, Money amount) {
        requirePositive(amount);
        return new Posting(account, amount.negate());
    }

    public Posting negate() {
        return new Posting(account, amount.negate());
    }

    private static void requirePositive(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("차변 대변 금액은 양수로 넘깁니다.: " + amount);
        }
    }
}
