package com.example.demo.payment.application.service;

import com.example.demo.payment.application.port.in.PaymentUseCase;
import com.example.demo.payment.adapter.out.persistence.toss_client.TossPaymentClient;
import com.example.demo.payment.adapter.out.persistence.toss_client.dto.TossPaymentResponse;
import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentFailure;
import com.example.demo.payment.adapter.in.web.dto.PaymentConfirmRequest;
import com.example.demo.payment.adapter.in.web.dto.PaymentFailRequest;
import com.example.demo.payment.adapter.in.web.dto.PaymentFailureResponse;
import com.example.demo.payment.adapter.in.web.dto.PaymentResponse;
import com.example.demo.payment.adapter.out.persistence.PaymentFailureJpaRepository;
import com.example.demo.payment.adapter.out.persistence.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentFailureJpaRepository paymentFailureJpaRepository; // 질문 : 실무에서는 결제 실패 / 주문 실패처리를 어떻게 하는가?
    private final TossPaymentClient tossPaymentClient;


    @Override
    public List<PaymentResponse> getAll(Pageable pageable) {
        return paymentJpaRepository.findAll(pageable).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    public PaymentResponse confirm(PaymentConfirmRequest request) {
        /*
        payment_key에 unique 제약이 있어, 같은 결제를 두 번 승인하면 제약 위반으로 500에러가 난다.
        success.html이 새로고침될 때마다 confirm을 호출하므로 먼저 기존 승인 내역을 확인한다.
         */
        Optional<Payment> alreadyConfirmed = paymentJpaRepository.findByPaymentKey(request.paymentKey());
        if (alreadyConfirmed.isPresent()) {
            return PaymentResponse.from(alreadyConfirmed.get());
        }
        TossPaymentResponse tossPayment = tossPaymentClient.confirm(request);
        if (tossPayment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Toss 결제 승인 응답이 비어 있습니다.");
        }

        Payment payment = Payment.create(
                tossPayment.paymentKey(),
                tossPayment.orderId(),
                tossPayment.totalAmount()
        );

        LocalDateTime approvedAt = tossPayment.approvedAt() != null
                ? tossPayment.approvedAt().toLocalDateTime()
                : null;
        LocalDateTime requestedAt = tossPayment.requestedAt() != null
                ? tossPayment.requestedAt().toLocalDateTime()
                : null;

        payment.markConfirmed(tossPayment.method(), approvedAt, requestedAt);

        return PaymentResponse.from(paymentJpaRepository.save(payment));
    }

    @Override
    public PaymentFailureResponse recordFailure(PaymentFailRequest request) {
        PaymentFailure failure = PaymentFailure.create(
                request.orderId(),
                request.paymentKey(),
                request.code(),
                request.message(),
                request.amount(),
                request.rawPayload()
        );
        return PaymentFailureResponse.from(paymentFailureJpaRepository.save(failure));
    }
}
