package com.example.demo.payment.application.port.out;

import com.example.demo.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** 결제 저장 포트 — Spring Data 리포지토리 인터페이스를 출력 포트로 쓴다(구현체는 Spring Data 가 만든다). */
public interface PaymentPersistencePort extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentKey(String paymentKey);
}
