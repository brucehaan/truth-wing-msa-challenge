package com.example.demo.order.application.port.in;

import com.example.demo.order.application.port.in.dto.OrderCreateRequest;
import com.example.demo.order.application.port.in.dto.OrderResponse;
import com.example.demo.order.domain.OrderPaymentResult;

import java.time.Instant;
import java.util.List;

public interface OrderUseCase {
    OrderResponse create(OrderCreateRequest request);

    List<OrderResponse> getAll();

    /** 결제 승인 사실을 주문에 반영한다 — PaymentApproved 이벤트 리스너(adapter.in.event)가 호출한다. */
    OrderPaymentResult confirmPayment(String orderNo, long approvedAmount, Instant approvedAt);
}
