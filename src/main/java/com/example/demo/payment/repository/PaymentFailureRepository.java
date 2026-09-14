package com.example.demo.payment.repository;

import com.example.demo.payment.domain.PaymentFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentFailureRepository extends JpaRepository<PaymentFailure, UUID> {
}
