package com.example.demo.order.application;

import com.example.demo.order.domain.OrderPaymentResult;
import com.example.demo.order.presentation.dto.OrderCreateRequest;
import com.example.demo.order.presentation.dto.OrderResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderResponse create(OrderCreateRequest request);

    List<OrderResponse> getAll();

    List<OrderResponse> getSettlementCandidates(LocalDate settlementDate);

    /** 결제 승인 사실을 주문에 반영한다. PaymentApproved 이벤트 리스너가 호출한다. */
    OrderPaymentResult confirmPayment(String orderNo, long approvedAmount, Instant approvedAt);
}
