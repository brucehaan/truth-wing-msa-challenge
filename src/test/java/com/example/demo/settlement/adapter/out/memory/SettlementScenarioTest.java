package com.example.demo.settlement.adapter.out.memory;

import com.example.demo.settlement.application.service.IncompleteSourceException;
import com.example.demo.settlement.domain.fee.FeePolicy;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.ledger.AccountCode;
import com.example.demo.settlement.domain.ledger.AccountKind;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SettlementScenarioTest {

    static final LocalDate D15 = LocalDate.of(2026, 1, 15);
    static final LocalDate D16 = LocalDate.of(2026, 1, 16);
    static final LocalDate D20 = LocalDate.of(2026, 1, 20);

    SettlementFixture fixture = new SettlementFixture();

    @Test
    @DisplayName("통제 합계가 맞지 않으면 한 건도 적재하지 않는다")
    void 완결성_게이트() {
        fixture.policy(null, "0.05", LocalDate.of(2026, 1, 1), null);
        fixture.staging.stage(D15, new SaleFact("ORD-001", "pk-1", "S-100", Money.won(10_100), SettlementFixture.kst(2026, 1, 15, 10, 0)));
        fixture.staging.declare(D15, new ControlTotal(2, Money.won(30_000))); // 상류는 두 건이라고 선언

        assertThatThrownBy(() -> fixture.intake.ingest(D15)).isInstanceOf(IncompleteSourceException.class);
        assertThat(fixture.ledger.size()).isZero();
    }

    @Test
    @DisplayName("같은 날짜를 다시 수집해도 원장에는 한 번만 들어간다")
    void 수집_멱등() {
        fixture.policy(null, "0.05", LocalDate.of(2026, 1, 1), null);
        fixture.stageAndDeclare(D15, new SaleFact("ORD-001", "pk-1", "S-100", Money.won(10_000), SettlementFixture.kst(2026, 1, 15, 10, 0)),
                new SaleFact("ORD-002", "pk-2", "S-100", Money.won(20_000), SettlementFixture.kst(2026, 1, 15, 11, 0)));

        fixture.intake.ingest(D15);
        var second = fixture.intake.ingest(D15);

        assertThat(second.posted()).isZero();
        assertThat(second.skipped()).isEqualTo(2);
        assertThat(fixture.ledger.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("마감 후 도착한 거래는 오늘로 이월되지만 수수료는 거래일 정책을 쓴다")
    void 마감후_도착분_이월() {
        FeePolicy jan15 = fixture.policy(null, "0.05", LocalDate.of(2026, 1, 1), D16);   // 1/16 부터 요율 인상
        fixture.policy(null, "0.10", D16, null);
        fixture.stageAndDeclare(D15, new SaleFact("ORD-001", "pk-1", "S-100", Money.won(10_000), SettlementFixture.kst(2026, 1, 15, 10, 0)));
        fixture.intake.ingest(D15);
        fixture.close.close(D15);

        fixture.stageAndDeclare(D15, new SaleFact("ORD-LATE", "pk-9", "S-100", Money.won(10_000), SettlementFixture.kst(2026, 1, 15, 23, 0)));
        fixture.intake.ingest(D15);

        JournalEntry late = fixture.ledger.findBySourceKey(SourceKey.sale("ORD-LATE")).orElseThrow();
        assertThat(late.header().businessDate()).isEqualTo(D16);
        assertThat(late.header().feePolicyId()).isEqualTo(jan15.id());
        assertThat(fixture.closing.find("S-100", D15).orElseThrow().payable()).isEqualTo(Money.won(9_500));
    }

    @Test
    @DisplayName("부분 환불이 여러 번이어도 판매 수수료는 1원도 남김없이 환입된다")
    void 부분환불_잔여_규칙() {
        fixture.policy(null, "0.033", LocalDate.of(2026, 1, 1), null);
        fixture.stageAndDeclare(D15, new SaleFact("ORD-P", "pk-p", "S-100", Money.won(1_000), SettlementFixture.kst(2026, 1, 15, 10, 0)));
        fixture.intake.ingest(D15);

        Money returned = Money.ZERO;
        for (var part : List.of(Map.entry("RF-A", 333L), Map.entry("RF-B", 333L), Map.entry("RF-C", 334L))) {
            JournalEntry e = fixture.refund.refund(new RefundFact(part.getKey(), "ORD-P", Money.won(part.getValue()),
                    SettlementFixture.kst(2026, 1, 15, 13, 0)));
            returned = returned.plus(e.sumFor(AccountCode.of(AccountKind.COMMISSION_REVENUE)));
        }

        assertThat(returned).isEqualTo(Money.won(33));
        assertThat(fixture.ledger.sumPostings(AccountCode.sellerPayable("S-100"), D15).isZero()).isTrue();
    }

    @Test
    @DisplayName("도메인을 우회해 원장이 오염되면 시산표가 깨져 마감되지 않는다")
    void 시산표_게이트() {
        fixture.policy(null, "0.05", LocalDate.of(2026, 1, 1), null);
        fixture.stageAndDeclare(D15, new SaleFact("ORD-001", "pk-1", "S-100", Money.won(10_000), SettlementFixture.kst(2026, 1, 15, 10, 0)));
        fixture.intake.ingest(D15);
        fixture.ledger.tamperDirectly(D15, Money.won(1));

        assertThatThrownBy(() -> fixture.close.close(D15)).isInstanceOf(IllegalStateException.class);
        assertThat(fixture.closing.findByDate(D15)).isEmpty();
    }
}
