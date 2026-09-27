package com.example.demo.settlement.application.port.out;

/**
 * 트랜잭셔널 아웃박스. 원장 및 마감 결과와 같은 트린잭션에 이벤트를 기록한다.
 * 이 이벤트는 "파생 데이터" (조회 모델, 지급 트리거)용이다. 원장 입력 경로가 아니다.
 */
public interface OutboxPort {
    void append(String eventType, String aggregateKey, String payload);
}
