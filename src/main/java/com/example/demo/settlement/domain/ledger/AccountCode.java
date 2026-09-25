package com.example.demo.settlement.domain.ledger;

import java.util.Objects;

/**
 * 계정 식별자. 판매자별 계정은 ownerId(sellerId)가 필수다.
 * @param kind
 * @param ownerId
 */
public record AccountCode(
        AccountKind kind,
        String ownerId
) {
        public AccountCode {
            if (kind.isPerSeller() && (ownerId == null || ownerId.isBlank())) {
                throw new IllegalArgumentException(kind + " 계정은 ownerId가 필요합니다.");
            }

            if (!kind.isPerSeller() && ownerId != null) {
                throw new IllegalArgumentException(kind + " 계정은 ownerId를 가질 수 없습니다.");
            }
        }

        public static AccountCode of(AccountKind kind) {
            return new AccountCode(kind, null);
        }

        public static AccountCode sellerPayable(String sellerId) {
            return new AccountCode(AccountKind.SELLER_PAYABLE, sellerId);
        }
}
