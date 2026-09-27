package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;

import java.util.List;

/**
 * 각 컨텍스트가 "내가 발행하는 이벤트 타입"을 등록하는 빈.
 * 공통 모듈이 컨텍스트의 이벤트 클래스를 import 하지 않게 하려고(의존 방향: 컨텍스트 → 공통) 이 방식을 쓴다.
 */
public record EventTypeRegistration(List<Class<? extends IntegrationEvent>> types) {

    public EventTypeRegistration {
        types = List.copyOf(types);
    }

    @SafeVarargs
    public static EventTypeRegistration of(Class<? extends IntegrationEvent>... types) {
        return new EventTypeRegistration(List.of(types));
    }
}
