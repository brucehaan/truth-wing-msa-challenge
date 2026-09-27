package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.published.SettlementEvent;

/**
 * 트랜잭셔널 아웃박스. 마감 결과와 같은 트랜잭션에 이벤트를 기록한다.
 * 이 이벤트는 "파생 데이터"(CQRS 조회 모델, 향후 지급 트리거)용이다 — 원장 입력 경로가 아니다.
 *
 * <p>2주차 변경: 문자열 payload 를 직접 조립하던 방식에서 타입 있는 이벤트로 바꿨다.
 * 소비자(조회 모델 프로젝터)가 JSON 모양을 추측하지 않고 같은 타입을 역직렬화할 수 있게 하기 위해서다.</p>
 */
public interface OutboxPort {
    void append(SettlementEvent event);
}
