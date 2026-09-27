package com.example.demo.common.event;

/**
 * 이벤트를 "지금 진행 중인 비즈니스 트랜잭션 안에" 기록하는 포트 — 트랜잭셔널 아웃박스의 입구.
 *
 * <p>브로커로 바로 보내지 않고 같은 DB 트랜잭션에 기록하는 이유: 상태 변경은 커밋됐는데 이벤트 전송이 실패하거나,
 * 전송은 됐는데 상태 변경이 롤백되는 "이중 쓰기(dual write)" 불일치를 구조적으로 없애기 위해서다.
 * 실제 전달은 커밋 이후 릴레이가 맡는다.</p>
 */
public interface EventRecorder {

    void record(IntegrationEvent event);
}
