package com.example.demo.order.service;

import com.example.demo.order.dto.OrderCreateRequest;
import com.example.demo.order.dto.OrderResponse;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderResponse create(OrderCreateRequest request);

    List<OrderResponse> getAll();

    List<OrderResponse> getSettlementCandidates(LocalDate settlementDate);
}
