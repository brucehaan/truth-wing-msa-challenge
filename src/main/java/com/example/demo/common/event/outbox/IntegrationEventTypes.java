package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 논리적 이벤트 이름 → 클래스 레지스트리. 아웃박스에서 꺼낸 JSON 을 어떤 타입으로 되돌릴지 정한다.
 * 이름이 겹치면 기동 시점에 실패시킨다 — 잘못된 타입으로 역직렬화되는 사고를 런타임까지 미루지 않기 위해서다.
 */
public final class IntegrationEventTypes {

    private final Map<String, Class<? extends IntegrationEvent>> byName = new HashMap<>();

    public IntegrationEventTypes(Collection<EventTypeRegistration> registrations) {
        for (EventTypeRegistration registration : registrations) {
            for (Class<? extends IntegrationEvent> type : registration.types()) {
                Class<? extends IntegrationEvent> previous = byName.putIfAbsent(nameOf(type), type);
                if (previous != null && previous != type) {
                    throw new IllegalStateException("이벤트 이름이 겹칩니다: " + nameOf(type)
                            + " (" + previous.getName() + ", " + type.getName() + ")");
                }
            }
        }
    }

    public Class<? extends IntegrationEvent> classOf(String eventType) {
        Class<? extends IntegrationEvent> type = byName.get(eventType);
        if (type == null) {
            throw new IllegalArgumentException("등록되지 않은 이벤트 타입입니다: " + eventType);
        }
        return type;
    }

    /** {@link IntegrationEvent#eventType()} 의 기본 구현과 같은 규칙이어야 한다. */
    public static String nameOf(Class<? extends IntegrationEvent> type) {
        return type.getSimpleName();
    }
}
