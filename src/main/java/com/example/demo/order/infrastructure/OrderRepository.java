package com.example.demo.order.infrastructure;

import com.example.demo.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    default List<Order> findUnsettledPaidOrders(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return findByStatusAndSettledFalseAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                "PAID",
                fromInclusive,
                toExclusive
        );
    }

    List<Order> findByStatusAndSettledFalseAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            String status,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    );
}
