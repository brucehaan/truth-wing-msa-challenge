package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.ledger.AccountCode;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 원장 저장소 포트. 구현체는 append만 제공한다 - update/delete는 계약에 없다.
 */
public interface LedgerPort {
    boolean exists(SourceKey key);

    /**
     * 같은 sourceKey가 없을 때만 적재하고 true, 이미 있으면 아무것도 하지 않고 false.
     * 예외로 알리지 않는 이유 : PostgreSQL은 제약 위반이 나면 트랜잭션 전체가 abort 되어
     * 같은 트랜잭션(=같은 청크)의 이후 문장이 모두 실패한다.
     * 구현체는 INSERT ... ON CONFLICT DO NOTHING으로 만든다.
     */
    boolean appendIfAbsent(JournalEntry entry);

    Optional<JournalEntry> findBySourceKey(SourceKey key);

    List<JournalEntry> findByOrderNo(String orderNo);

    /* 특정 계정 x 정산일의 부호 있는 합계 */
    Money sumPostings(AccountCode account, LocalDate businessDate);

    /* 시산표 - 그날 모든 분개의 합. 복식부기가 지켜졌다면 항상 0이다. */
    Money trialBalance(LocalDate businessDate);

    Set<String> sellerWithPostingsOn(LocalDate businessDate);
}
