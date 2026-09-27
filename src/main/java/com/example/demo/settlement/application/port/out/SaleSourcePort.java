package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.time.LocalDate;
import java.util.List;

/**
 * 판매 사실의 원천(상류). 정산의 언어(SaleFact, ControlTotal)로 정의한다.
 * 구현체는 상류의 언어를 이 언어로 번역하는 ACL 이다 — 지금은 주문 모듈(adapter.out.order), 서비스를 나누면 gRPC 클라이언트.
 */
public interface SaleSourcePort {

    /** D일(Asia/Seoul)에 결제된 판매 사실. */
    List<SaleFact> paidOn(LocalDate occurredDate);

    /** 상류가 목록과 독립적으로 센 D일 통제 합계. */
    ControlTotal declaredTotal(LocalDate occurredDate);
}
