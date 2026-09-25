package com.example.demo.settlement.domain.intake;

import com.example.demo.settlement.domain.money.Money;

import java.util.Collection;

/**
 * 통제 합계 (control total) - 건수와 금액 합
 * 상류가 "D일에 N건, 총 X원을 보냈다"고 선언한 값과 우리가 받은 값을 대조해 "못 받은 것이 없다"를 증명한다.
 * 배치 수집이 이벤트 수집보다 정산에 맞는 이유가 이 증명 가능성이다.
 */
public record ControlTotal(
        long count,
        Money sum
) {
    public static ControlTotal of(Collection<Money> amounts) {
        Money sum = amounts.stream().reduce(Money.ZERO, Money::plus);
        return new ControlTotal(amounts.size(), sum);
    }
}
