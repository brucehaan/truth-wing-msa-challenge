package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.CloseDayUseCase;
import com.example.demo.settlement.application.port.out.LedgerPort;
import com.example.demo.settlement.application.port.out.OutboxPort;
import com.example.demo.settlement.application.port.out.SellerStatementPort;
import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.closing.SellerStatement;
import com.example.demo.settlement.domain.closing.SettlementDay;
import com.example.demo.settlement.domain.ledger.AccountCode;
import com.example.demo.settlement.domain.ledger.AccountKind;
import com.example.demo.settlement.domain.money.Money;

import com.example.demo.settlement.published.SellerStatementIssued;
import com.example.demo.settlement.published.SettlementDayClosed;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

/**
 * 마감 유스케이스.
 * 게이트 판정은 SettlementDay(Aggregate)가 하고, 재실행 안전성은 이 서비스가 책임진다.
 * Aggregate 는 "두 번 마감"을 예외로 막고, 유스케이스는 이미 마감된 날을 조용히 건너뛴다.
 */
public class CloseService implements CloseDayUseCase {

    static final Set<String> REQUIRED_SOURCES = Set.of(IntakeService.SOURCE);

    private final SettlementDayPort dayPort;
    private final LedgerPort ledgerPort;
    private final SellerStatementPort statementPort;
    private final OutboxPort outboxPort;
    private final Clock clock;

    public CloseService(SettlementDayPort dayPort, LedgerPort ledgerPort, SellerStatementPort statementPort,
                        OutboxPort outboxPort, Clock clock) {
        this.dayPort = dayPort;
        this.ledgerPort = ledgerPort;
        this.statementPort = statementPort;
        this.outboxPort = outboxPort;
        this.clock = clock;
    }

    @Override
    public CloseResult close(LocalDate date) {
        SettlementDay day = dayPort.find(date).orElseGet(() -> SettlementDay.open(date));
        if (day.isClosed()) {
            return new CloseResult(date, true, statementPort.findByDate(date).size());
        }

        // 게이트 — 실패하면 여기서 예외. 판매자 정산서는 한 장도 만들어지지 않는다
        day.close(REQUIRED_SOURCES, ledgerPort.trialBalance(date), clock.instant());

        int issued = 0;
        for (String sellerId : ledgerPort.sellersWithPostingsOn(date)) {
            if (statementPort.find(sellerId, date).isPresent()) {
                continue;                                           // 파티션 재시작 대비
            }
            Money signed = ledgerPort.sumPostings(AccountCode.sellerPayable(sellerId), date);
            Money payable = AccountKind.SELLER_PAYABLE.type().normalBalance(signed);
            statementPort.save(new SellerStatement(sellerId, date, payable, day.closedAt()));
            outboxPort.append(SellerStatementIssued.of(sellerId, date, payable.amount(), day.closedAt()));   // CQRS 조회 모델 입력
            issued++;
        }
        dayPort.save(day);
        outboxPort.append(SettlementDayClosed.of(date, day.closedAt(), issued));
        return new CloseResult(date, false, issued);
    }
}
