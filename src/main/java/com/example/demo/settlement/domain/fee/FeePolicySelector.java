package com.example.demo.settlement.domain.fee;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 도메인 서비스 - 판매자 전용 정책을 기본 정책보다 우선한다.
 */
public final class FeePolicySelector {
    public FeePolicy select(Collection<FeePolicy> candidates, String sellerId, LocalDate on) {
        List<FeePolicy> sellerSpecific = candidates.stream()
                .filter(p -> sellerId.equals(p.sellerId()) && p.isEffectiveOn(on))
                .toList();
        if (sellerSpecific.size() > 1) {
            throw new IllegalStateException("판매자 전용 정책의 기간이 겹칩니다. sellerId=" + sellerId + ", on=" + on);
        }
        if (sellerSpecific.size() == 1) {
            return sellerSpecific.get(0);
        }
        List<FeePolicy> defaults = candidates.stream()
                .filter(p -> !p.isSellerSpecific() && p.isEffectiveOn(on))
                .toList();
        if (defaults.size() != 1) {
            // 0건이면 0%를 적용하게 되고, 2건 이상이면 금액이 비결정적이 된다
            throw new IllegalStateException("적용 가능한 기본 정책이 정확히 1건이어야 합니다. 발견=" + defaults.size() + ", on=" + on);
        }
        return defaults.get(0);
    }
}
