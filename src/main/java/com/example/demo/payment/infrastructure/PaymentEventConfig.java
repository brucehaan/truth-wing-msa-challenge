package com.example.demo.payment.infrastructure;

import com.example.demo.common.event.outbox.EventTypeRegistration;
import com.example.demo.payment.published.PaymentApproved;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 결제 컨텍스트가 아웃박스로 발행하는 이벤트 타입을 등록한다. */
@Configuration
public class PaymentEventConfig {

    @Bean
    public EventTypeRegistration paymentEventTypes() {
        return EventTypeRegistration.of(PaymentApproved.class);
    }
}
