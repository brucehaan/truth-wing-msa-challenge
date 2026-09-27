package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 같은 JVM 의 {@code @EventListener} 에게 동기로 전달한다.
 * 동기 호출이므로 리스너는 릴레이가 연 트랜잭션 안에서 실행되고, 리스너가 예외를 던지면 발행 표시까지 함께 롤백된다.
 */
@Component
public class SpringEventDispatcher implements EventDispatcher {

    private final ApplicationEventPublisher publisher;

    public SpringEventDispatcher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void dispatch(IntegrationEvent event) {
        publisher.publishEvent(event);
    }
}
