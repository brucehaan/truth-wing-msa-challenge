package com.example.demo.settlement.query;

import com.example.demo.settlement.published.SellerStatementIssued;
import com.example.demo.settlement.published.SettlementDayClosed;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 프로젝터 — 정산이 발행한 이벤트(Published Language)로 조회 모델을 만든다.
 *
 * <p>의존 방향: query → settlement.published 만. settlement.domain / settlement.application 을 모른다(아키텍처 테스트로 강제).
 * 그래서 조회 모델을 버리고 아웃박스 이벤트를 처음부터 다시 흘려 재구성할 수 있다.</p>
 *
 * <p>아웃박스 릴레이가 연 트랜잭션 안에서 동기로 실행된다. 여기서 예외가 나면 이벤트는 발행되지 않은 것으로 남아 재시도된다.</p>
 */
@Component
public class SettlementReadModelProjector {

    private final SettlementReadModel readModel;

    public SettlementReadModelProjector(SettlementReadModel readModel) {
        this.readModel = readModel;
    }

    @EventListener
    public void on(SellerStatementIssued event) {
        readModel.upsertStatement(event.sellerId(), event.businessDate(), event.payable(), event.closedAt());
        readModel.refreshSellerSummary(event.sellerId());
        readModel.refreshDay(event.businessDate(), null);
    }

    @EventListener
    public void on(SettlementDayClosed event) {
        readModel.refreshDay(event.businessDate(), event.closedAt());
    }
}
