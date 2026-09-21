package com.example.demo.payment.application.port.out;

import com.example.demo.payment.domain.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentPersistencePort {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    List<Payment> findAll();

    void delete(Payment payment);

}
