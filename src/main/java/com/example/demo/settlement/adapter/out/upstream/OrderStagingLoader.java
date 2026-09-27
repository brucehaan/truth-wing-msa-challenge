package com.example.demo.settlement.adapter.out.upstream;

import com.example.demo.order.application.port.in.OrderControlTotal;
import com.example.demo.order.application.port.in.OrderSettlementFact;
import com.example.demo.order.application.port.in.OrderUseCase;
import com.example.demo.settlement.application.service.IntakeService;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * 주문 → 정산 수집 적재기 (README Step 9, 3장 adapter/out/upstream).
 * 지금은 같은 저장소의 주문 모듈을 부르고, 대규모에서는 파일·CDC 적재기로 교체한다.
 *
 * 주문의 공개 API(인바운드 포트 OrderUseCase)만 쓴다 — 주문 엔티티·JPA 리포지토리를 import 하지 않는다(ArchUnit 규칙).
 * 번역 규칙: 판매자 UUID → 문자열, 금액 BigDecimal → 원 단위 long(소수가 있으면 예외 — KRW 에 소수는 데이터 오류),
 *           paidAt(LocalDateTime) → Asia/Seoul 로 해석한 절대 시각.
 */
@Component
@RequiredArgsConstructor
public class OrderStagingLoader {

    private static final String SOURCE = IntakeService.SOURCE;

    private final OrderUseCase orderUseCase;
    private final JdbcTemplate jdbc;

    /* 배치 Step 1. 스테이징 적재와 매니페스트(상류 선언 통제 합계) 기록. 재실행해도 같은 결과. */
    public void load(LocalDate date) {
        for (OrderSettlementFact f : orderUseCase.exportPaidFacts(date)) {
            jdbc.update("""
                    INSERT INTO settlement.inbound_sale_fact
                        (source, order_no, occurred_date, payment_key, seller_id, gross, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (source, order_no) DO NOTHING
                    """, SOURCE, f.orderNo(), date, null, f.sellerId().toString(),   // paymentKey 는 아직 주문에 없다
                    f.grossAmount().longValueExact(),
                    Timestamp.from(f.paidAt().atZone(BusinessCalendar.ZONE).toInstant()));
        }
        OrderControlTotal t = orderUseCase.paidControlTotal(date);
        jdbc.update("""
                INSERT INTO settlement.inbound_manifest (source, occurred_date, record_count, amount_sum)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (source, occurred_date)
                DO UPDATE SET record_count = EXCLUDED.record_count,
                              amount_sum   = EXCLUDED.amount_sum,
                              declared_at  = now()
                """, SOURCE, date, t.count(), t.sum().longValueExact());
    }
}
