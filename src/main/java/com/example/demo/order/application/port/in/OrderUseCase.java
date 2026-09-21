package com.example.demo.order.application.port.in;

import com.example.demo.order.adapter.in.web.dto.OrderCreateRequest;
import com.example.demo.order.adapter.in.web.dto.OrderResponse;

import java.time.LocalDate;
import java.util.List;

public interface OrderUseCase {
    OrderResponse create(OrderCreateRequest request);

    List<OrderResponse> getAll();

    List<OrderResponse> getSettlementCandidates(LocalDate settlementDate);
}
