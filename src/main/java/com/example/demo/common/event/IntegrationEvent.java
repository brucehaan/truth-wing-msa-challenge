package com.example.demo.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 컨텍스트 경계를 넘어가는 이벤트(Integration Event)의 공통 계약 — 공유 커널.
 *
 * <p>도메인 내부 상태 변화가 아니라 "다른 컨텍스트가 알아도 되는 사실"만 이 타입으로 발행한다.
 * 순수 자바 타입만 쓰므로 어느 계층에서 참조해도 스프링에 묶이지 않는다.</p>
 *
 * <ul>
 *   <li>{@code eventId} — 소비자 멱등 처리의 키. 같은 이벤트가 두 번 전달돼도 한 번만 반영한다</li>
 *   <li>{@code occurredAt} — 사실이 일어난 시각(발행 시각이 아니다)</li>
 *   <li>{@code aggregateKey} — 같은 대상에 대한 이벤트를 묶는 키(주문번호, 판매자:정산일 등)</li>
 * </ul>
 */
public interface IntegrationEvent {

    UUID eventId();

    Instant occurredAt();

    String aggregateKey();

    /** 아웃박스에 저장되는 논리적 이벤트 이름. 클래스 이름을 바꾸면 이 값도 바뀌므로 신중히 다룬다. */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
