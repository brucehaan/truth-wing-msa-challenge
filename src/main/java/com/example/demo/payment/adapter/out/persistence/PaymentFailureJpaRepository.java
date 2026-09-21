package com.example.demo.payment.adapter.out.persistence;

import com.example.demo.payment.domain.PaymentFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentFailureJpaRepository extends JpaRepository<PaymentFailure, UUID> {
}
