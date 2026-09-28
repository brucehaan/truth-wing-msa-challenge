package com.example.demo.payment.application.port.out;

import com.example.demo.payment.domain.PaymentFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 결제 실패 기록 저장 포트. */
public interface PaymentFailurePersistencePort extends JpaRepository<PaymentFailure, UUID> {
}
