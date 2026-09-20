package com.example.demo.order.application;

import com.example.demo.order.domain.Order;
import com.example.demo.order.presentation.dto.OrderCreateRequest;
import com.example.demo.order.presentation.dto.OrderResponse;
import com.example.demo.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true) // 질문 : 실무에서도 이렇게 클래스 단에다가 transactional 어노테이션을 붙이나?
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        UUID actorId = resolveActorId(request);

        Order order = Order.create(
                request.orderNo(),
                request.buyerId(),
                request.sellerId(),
                request.productId(),
                request.quantity(),
                request.grossAmount(),
                request.feeAmount(),
                request.refundAmount(),
                request.status(),
                request.paidAt(),
                actorId
        );
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    public List<OrderResponse> getSettlementCandidates(LocalDate settlementDate) {
        LocalDate targetDate = settlementDate == null ? LocalDate.now() : settlementDate;
        LocalDateTime fromInclusive = targetDate.atStartOfDay();
        LocalDateTime toExclusive = fromInclusive.plusDays(1);

        return orderRepository.findUnsettledPaidOrders(fromInclusive, toExclusive).stream()
                .map(OrderResponse::from)
                .toList();
    }

    private UUID resolveActorId(OrderCreateRequest request) {
        if (request.actorId() != null) return request.actorId();
        if (request.buyerId() != null) return request.buyerId();
        return UUID.randomUUID();
    }
}
