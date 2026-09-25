package com.example.demo.settlement.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 원(KRW) 단위 정수 금액
 * KRW는 ISO 4217 소수 자릿수가 0 이므로 long 으로 충분하다.
 * 이렇게 함으로써 BigDecimal scale 불일치 (9500 vs 9500.00)로 equals가 깨지는 버그가 원천적으로 생기지 않는다.
 */
public record Money(
        long amount
) implements Comparable<Money> {

    public static final Money ZERO = new Money(0L);

    public static Money won(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(amount, other.amount)); // 오버플로는 예외로
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(amount, other.amount));
    }

    public Money negate() {
        return new Money(Math.negateExact(amount));
    }

    /* 비율 적용 - 소수 연산이 일어나는 유일한 지점. 반올림 규칙은 호출자(정책)가 정한다. */
    public Money multiply(BigDecimal rate, RoundingMode rounding) {
        BigDecimal result = BigDecimal.valueOf(amount).multiply(rate).setScale(0, rounding);
        return new Money(result.longValueExact());
    }

    public boolean isZero() { return amount == 0L; }
    public boolean isPositive() { return amount > 0L; }
    public boolean isNegative() { return amount < 0L; }

    @Override
    public String toString() {
        return amount + "원";
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(amount, other.amount);
    }
}
