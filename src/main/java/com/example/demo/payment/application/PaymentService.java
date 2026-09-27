package com.example.demo.payment.application;

import com.example.demo.payment.presentation.dto.PaymentConfirmRequest;
import com.example.demo.payment.presentation.dto.PaymentFailRequest;
import com.example.demo.payment.presentation.dto.PaymentFailureResponse;
import com.example.demo.payment.presentation.dto.PaymentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    List<PaymentResponse> getAll(Pageable pageable);

    PaymentResponse confirm(PaymentConfirmRequest request);

    PaymentFailureResponse recordFailure(PaymentFailRequest request);
}
