package com.example.demo.order.application;

import com.example.demo.order.domain.Order;
import com.example.demo.order.domain.OrderPaymentResult;
import com.example.demo.order.presentation.dto.OrderCreateRequest;
import com.example.demo.order.presentation.dto.OrderResponse;
import com.example.demo.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true) // 질문 : 실무에서도 이렇게 클래스 단에다가 transactional 어노테이션을 붙이나?
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 이벤트로 들어온 변경의 수정자 — 사람이 아니라 결제 시스템이다. */
    private static final UUID PAYMENT_SYSTEM_ACTOR = new UUID(0L, 0L);

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

    /**
     * 결제 승인 반영. 트랜잭션은 이벤트를 전달한 아웃박스 릴레이의 트랜잭션에 참여한다 —
     * 주문 변경과 "이벤트 처리 완료" 표시가 함께 커밋되거나 함께 롤백된다.
     */
    @Override
    @Transactional
    public OrderPaymentResult confirmPayment(String orderNo, long approvedAmount, Instant approvedAt) {
        LocalDateTime approvedAtKst = LocalDateTime.ofInstant(approvedAt, KST);
        return orderRepository.findByOrderNo(orderNo)
                .map(order -> order.confirmPayment(approvedAmount, approvedAtKst, PAYMENT_SYSTEM_ACTOR))
                .orElse(OrderPaymentResult.ORDER_NOT_FOUND);
    }

    private UUID resolveActorId(OrderCreateRequest request) {
        if (request.actorId() != null) return request.actorId();
        if (request.buyerId() != null) return request.buyerId();
        return UUID.randomUUID();
    }
}
