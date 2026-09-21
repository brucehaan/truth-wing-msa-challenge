package com.example.demo.payment.adapter.out.persistence;

import com.example.demo.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentKey(String paymentKey);
}
