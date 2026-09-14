package com.example.demo.payment.service;

import com.example.demo.payment.dto.PaymentConfirmRequest;
import com.example.demo.payment.dto.PaymentFailRequest;
import com.example.demo.payment.dto.PaymentFailureResponse;
import com.example.demo.payment.dto.PaymentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    List<PaymentResponse> getAll(Pageable pageable);

    PaymentResponse confirm(PaymentConfirmRequest request);

    PaymentFailureResponse recordFailure(PaymentFailRequest request);
}
