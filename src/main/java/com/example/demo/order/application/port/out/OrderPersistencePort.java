package com.example.demo.order.application.port.out;

import com.example.demo.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 주문 저장 포트. 지원 모듈(주문·결제·상품)은 Spring Data 리포지토리 인터페이스를 그대로 출력 포트로 쓴다 —
 * 구현체(어댑터)는 Spring Data 가 실행 시점에 만든다. 서비스는 이 포트만 알고 어댑터 패키지를 모른다.
 */
public interface OrderPersistencePort extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNo(String orderNo);

    /* 정산 제공용: status 로 거르지 않고 paidAt 반열림 구간 [D, D+1). 0원 주문은 목록·합계 양쪽에서 같은 기준으로 뺀다 */
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
