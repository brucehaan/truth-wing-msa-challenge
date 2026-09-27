package com.example.demo.common.event.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableScheduling
public class OutboxConfig {

    @Bean
    public IntegrationEventTypes integrationEventTypes(List<EventTypeRegistration> registrations) {
        return new IntegrationEventTypes(registrations);
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxStore store, IntegrationEventCodec codec, EventDispatcher dispatcher,
                                   TxRunner tx, Clock clock,
                                   @Value("${outbox.relay.max-attempts:10}") int maxAttempts) {
        return new OutboxRelay(store, codec, dispatcher, tx, clock, maxAttempts,
                Duration.ofSeconds(1), Duration.ofMinutes(10));
    }
}
