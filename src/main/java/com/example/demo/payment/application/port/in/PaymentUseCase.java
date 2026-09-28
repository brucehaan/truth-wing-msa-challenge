package com.example.demo.payment.application.port.in;

import com.example.demo.payment.application.port.in.dto.PaymentConfirmRequest;
import com.example.demo.payment.application.port.in.dto.PaymentFailRequest;
import com.example.demo.payment.application.port.in.dto.PaymentFailureResponse;
import com.example.demo.payment.application.port.in.dto.PaymentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentUseCase {
    List<PaymentResponse> getAll(Pageable pageable);

    PaymentResponse confirm(PaymentConfirmRequest request);

    PaymentFailureResponse recordFailure(PaymentFailRequest request);
}
