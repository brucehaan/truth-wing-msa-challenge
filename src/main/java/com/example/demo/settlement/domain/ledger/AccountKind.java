package com.example.demo.settlement.domain.ledger;

/**
 * 계정과목(Chart of Accounts). 정산 도메인이 다루는 돈의 자리
 */
public enum AccountKind {
    PG_RECEIVABLE(AccountType.ASSET, false), // PG 미수금 - PG가 플랫폼에 줄 돈
    SELLER_PAYABLE(AccountType.LIABILITY, true), // 판매자 미지급금 - 플랫폼이 판매자에게 줄 돈 (판매자별)
    COMMISSION_REVENUE(AccountType.REVENUE, false), // 판매수수료 수익 - 플랫폼이 판매자에게 받는 수수료
    PG_FEE_EXPENSE(AccountType.EXPENSE, false), // PG 수수료 비용 - PG가 플랫폼에 부과하는 수수료
    CASH(AccountType.ASSET, false); // 보통예금

    private final AccountType type;
    private final boolean perSeller;

    AccountKind(AccountType type, boolean perSeller) {
        this.type = type;
        this.perSeller = perSeller;
    }

    public AccountType type() { return type; }
    public boolean isPerSeller() { return perSeller; }
}
