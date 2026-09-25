package com.example.demo.settlement.domain.fee;

import com.example.demo.settlement.domain.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 판매 수수료 정책. sellerId가 null이면 기본 정책
 * 적용 기간은 반열림 [effectiveFrom, effectiveTo) - 교체일에 두 정책이 동시에 유효해지지 않는다.
 * 반올림 규칙은 정책이 소유한다 - 계약마다 다를 수 있기 때문이다.
 */
public record FeePolicy(
        UUID id,
        String sellerId,
        BigDecimal rate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        RoundingMode rounding
) {
    public FeePolicy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(rounding, "rounding");

        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("수수료율은 0이상 1이하여야 합니다.");
        }
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo는 effectiveFrom보다 뒤여야 한다.");
        }
    }

    public boolean isEffectiveOn(LocalDate date) {
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || date.isBefore(effectiveTo));
    }

    public boolean isSellerSpecific() {
        return sellerId != null;
    }

    public Money commissionOf(Money gross) {
        return gross.multiply(rate, rounding);
    }
}
