package com.example.demo.order.adapter.in.event;

import com.example.demo.common.event.outbox.ProcessedEvents;
import com.example.demo.order.application.port.in.OrderUseCase;
import com.example.demo.order.domain.OrderPaymentResult;
import com.example.demo.payment.published.PaymentApproved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 주문의 이벤트 입구(인바운드 어댑터) — 컨트롤러가 HTTP 요청을 받듯 이 클래스는 이벤트를 받아 유스케이스 포트를 부른다.
 * 결제는 주문을 모른다. 결제는 PaymentApproved 를 발행할 뿐이고, 주문이 스스로 반영한다(코레오그래피).
 * 아웃박스 전달은 at-least-once 라 같은 이벤트가 다시 올 수 있다 → processed_event 로 멱등 처리한다.
 */
@Component
public class PaymentApprovedListener {

    static final String CONSUMER = "order.payment-approved";
    private static final Logger log = LoggerFactory.getLogger(PaymentApprovedListener.class);

    private final ProcessedEvents processedEvents;
    private final OrderUseCase orderUseCase;

    public PaymentApprovedListener(ProcessedEvents processedEvents, OrderUseCase orderUseCase) {
        this.processedEvents = processedEvents;
        this.orderUseCase = orderUseCase;
    }

    @EventListener
    public void on(PaymentApproved event) {
        if (!processedEvents.markIfFirst(CONSUMER, event.eventId())) {
            log.debug("이미 처리한 PaymentApproved 입니다. eventId={}", event.eventId());
            return;
        }
        OrderPaymentResult result = orderUseCase.confirmPayment(event.orderNo(), event.amount(), event.approvedAt());
        switch (result) {
            case CONFIRMED, ALREADY_CONFIRMED ->
                    log.info("주문 결제 확정: orderNo={}, result={}", event.orderNo(), result);
            case AMOUNT_MISMATCH, NOT_PAYABLE, ORDER_NOT_FOUND ->
                    // 재시도해도 결과가 같다 — 예외로 릴레이를 막지 않고, 운영자가 볼 수 있게 남긴다
                    log.warn("결제 승인을 주문에 반영하지 못했습니다: orderNo={}, paymentKey={}, amount={}, result={}",
                            event.orderNo(), event.paymentKey(), event.amount(), result);
        }
    }
}
