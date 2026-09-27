package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 기반 코덱. 이벤트는 모두 record 이고, java.time 타입은 Jackson 3 에 내장된 지원으로 ISO-8601 문자열이 된다.
 *
 * <p>스프링 부트가 만든 JsonMapper 빈 대신 전용 인스턴스를 쓴다. 웹 API 용 직렬화 설정(날짜 형식, 네이밍 전략 등)이
 * 바뀌어도 이미 저장된 아웃박스 payload 의 해석이 흔들리지 않게 하기 위해서다.</p>
 */
@Component
public class JacksonIntegrationEventCodec implements IntegrationEventCodec {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final IntegrationEventTypes types;

    public JacksonIntegrationEventCodec(IntegrationEventTypes types) {
        this.types = types;
    }

    @Override
    public String encode(IntegrationEvent event) {
        return mapper.writeValueAsString(event);
    }

    @Override
    public IntegrationEvent decode(String eventType, String payload) {
        return mapper.readValue(payload, types.classOf(eventType));
    }
}
