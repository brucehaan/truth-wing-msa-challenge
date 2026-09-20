package com.example.demo.order.application;

import com.example.demo.order.presentation.dto.OrderCreateRequest;
import com.example.demo.order.presentation.dto.OrderResponse;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderResponse create(OrderCreateRequest request);

    List<OrderResponse> getAll();

    List<OrderResponse> getSettlementCandidates(LocalDate settlementDate);
}
