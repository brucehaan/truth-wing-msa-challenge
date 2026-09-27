package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;

/** 이벤트 ↔ 저장 형식(JSON) 변환. 형식 선택을 릴레이 로직에서 떼어 내기 위한 포트다. */
public interface IntegrationEventCodec {

    String encode(IntegrationEvent event);

    IntegrationEvent decode(String eventType, String payload);
}
