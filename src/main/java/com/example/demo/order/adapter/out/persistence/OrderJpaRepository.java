package com.example.demo.order.adapter.out.persistence;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

import com.example.demo.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

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

    /*
     * 정산 제공용 (README Step 9). status 로 거르지 않고 paidAt 반열림 구간 [D, D+1) 으로 뽑는다.
     * 0원 주문은 정산 사실이 아니므로 목록·합계 양쪽에서 같은 기준으로 뺀다
     * (SaleFact 는 양수 금액만 받고, 스테이징 테이블에도 gross > 0 CHECK 가 있다).
     */
    List<Order> findByPaidAtGreaterThanEqualAndPaidAtLessThanAndGrossAmountGreaterThanOrderByOrderNoAsc(
            LocalDateTime fromInclusive, LocalDateTime toExclusive, BigDecimal minExclusive);

    long countByPaidAtGreaterThanEqualAndPaidAtLessThanAndGrossAmountGreaterThan(
            LocalDateTime fromInclusive, LocalDateTime toExclusive, BigDecimal minExclusive);

    /* 판매 0건인 날 SUM 은 NULL 이다 — COALESCE 로 0 을 돌려준다 */
    @Query(value = """
            SELECT COALESCE(SUM(gross_amount), 0) FROM "order"
            WHERE paid_at >= :fromInclusive AND paid_at < :toExclusive AND gross_amount > 0
            """, nativeQuery = true)
    BigDecimal sumGrossAmountPaidBetween(@Param("fromInclusive") LocalDateTime fromInclusive,
                                         @Param("toExclusive") LocalDateTime toExclusive);
}
