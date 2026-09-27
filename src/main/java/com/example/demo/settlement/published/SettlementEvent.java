package com.example.demo.settlement.published;

import com.example.demo.common.event.IntegrationEvent;

/**
 * 정산 컨텍스트가 외부(조회 모델, 향후 지급)에 공개하는 이벤트 — Published Language.
 *
 * <p>sealed 로 닫아 둔 이유: 정산이 밖으로 내보내는 사실의 종류를 이 파일 하나에서 전부 볼 수 있어야
 * 소비자(CQRS 프로젝터)가 빠뜨리는 이벤트가 없는지 검토할 수 있다.</p>
 */
public sealed interface SettlementEvent extends IntegrationEvent
        permits SellerStatementIssued, SettlementDayClosed {
}
