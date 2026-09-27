package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;

/**
 * 이벤트를 소비자에게 전달하는 포트.
 * 지금(모놀리스)은 같은 JVM 의 리스너를 동기 호출하고({@link SpringEventDispatcher}),
 * 서비스를 분리하면 브로커로 보내는 구현으로 바꾼다. 발행하는 쪽 코드는 바뀌지 않는다.
 */
public interface EventDispatcher {

    void dispatch(IntegrationEvent event);
}
