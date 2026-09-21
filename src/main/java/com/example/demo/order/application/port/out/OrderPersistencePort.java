package com.example.demo.order.application.port.out;

import com.example.demo.order.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderPersistencePort {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    List<Order> findAll();

    void delete(Order order);
}
