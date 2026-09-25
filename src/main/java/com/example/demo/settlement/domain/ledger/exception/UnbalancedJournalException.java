package com.example.demo.settlement.domain.ledger.exception;

import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;

public class UnbalancedJournalException extends IllegalStateException{
    public UnbalancedJournalException(SourceKey key, Money sum) {
        super("차변과 대변이 맞지 않습니다. sourceKey=" + key.value() + ", 합계=" + sum);
    }
}
