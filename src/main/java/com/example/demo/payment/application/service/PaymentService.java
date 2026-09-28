package com.example.demo.payment.application.service;

import com.example.demo.common.event.EventRecorder;
import com.example.demo.payment.application.port.in.PaymentUseCase;
import com.example.demo.payment.application.port.in.dto.PaymentConfirmRequest;
import com.example.demo.payment.application.port.in.dto.PaymentFailRequest;
import com.example.demo.payment.application.port.in.dto.PaymentFailureResponse;
import com.example.demo.payment.application.port.in.dto.PaymentResponse;
import com.example.demo.payment.application.port.out.PaymentFailurePersistencePort;
import com.example.demo.payment.application.port.out.PaymentGateway;
import com.example.demo.payment.application.port.out.PaymentPersistencePort;
import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentApproval;
import com.example.demo.payment.domain.PaymentFailure;
import com.example.demo.payment.published.PaymentApproved;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * ACL: 토스를 직접 부르지 않고 PaymentGateway 포트만 안다.
 * EDA: 결제 저장과 PaymentApproved 기록을 한 트랜잭션으로 묶는다(트랜잭셔널 아웃박스). 주문 확정은 주문이 이벤트를 받아 스스로 한다.
 * 클래스 단 readOnly 를 두지 않는 이유: confirm 이 그것을 물려받으면 안쪽 TransactionTemplate 이 읽기 전용 트랜잭션에 참여해 저장이 사라진다.
 */
@Service
public class PaymentService implements PaymentUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PaymentPersistencePort paymentPersistencePort;
    private final PaymentFailurePersistencePort paymentFailurePersistencePort; // 질문 : 실무에서는 결제 실패 / 주문 실패처리를 어떻게 하는가?
    private final PaymentGateway paymentGateway;
    private final EventRecorder eventRecorder;
    private final TransactionTemplate tx;

    public PaymentService(PaymentPersistencePort paymentPersistencePort,
                          PaymentFailurePersistencePort paymentFailurePersistencePort,
                          PaymentGateway paymentGateway,
                          EventRecorder eventRecorder,
                          PlatformTransactionManager transactionManager) {
        this.paymentPersistencePort = paymentPersistencePort;
        this.paymentFailurePersistencePort = paymentFailurePersistencePort;
        this.paymentGateway = paymentGateway;
        this.eventRecorder = eventRecorder;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll(Pageable pageable) {
        return paymentPersistencePort.findAll(pageable).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * 1) 이미 승인된 결제면 그대로 돌려준다 — success 페이지 새로고침으로 다시 불려도 안전하게
     * 2) PG 승인(외부 HTTP)은 트랜잭션 밖에서 — 응답을 기다리는 동안 DB 커넥션을 잡지 않는다
     * 3) 결제 저장 + PaymentApproved 기록을 한 트랜잭션으로 — 둘 중 하나만 남는 경우가 없다
     */
    @Override
    public PaymentResponse confirm(PaymentConfirmRequest request) {
        if (request.paymentKey() == null || request.orderId() == null || request.amount() == null) {
            throw new IllegalArgumentException("paymentKey, orderId, amount 는 필수입니다.");
        }
        Optional<Payment> alreadyConfirmed = paymentPersistencePort.findByPaymentKey(request.paymentKey());
        if (alreadyConfirmed.isPresent()) {
            return PaymentResponse.from(alreadyConfirmed.get());
        }

        PaymentApproval approval = paymentGateway.approve(request.paymentKey(), request.orderId(), request.amount());

        Payment saved = tx.execute(status -> {
            Optional<Payment> raced = paymentPersistencePort.findByPaymentKey(approval.paymentKey());
            if (raced.isPresent()) {
                return raced.get();                                   // 같은 승인이 동시에 두 번 — 먼저 저장된 쪽을 돌려준다
            }
            Payment payment = Payment.create(approval.paymentKey(), approval.orderNo(), approval.amount());
            payment.markConfirmed(approval.method().name(), toKst(approval.approvedAt()), toKst(approval.requestedAt()));
            Payment persisted = paymentPersistencePort.save(payment);
            eventRecorder.record(PaymentApproved.of(approval));       // 같은 트랜잭션의 아웃박스
            return persisted;
        });
        return PaymentResponse.from(saved);
    }

    @Override
    @Transactional
    public PaymentFailureResponse recordFailure(PaymentFailRequest request) {
        PaymentFailure failure = PaymentFailure.create(
                request.orderId(),
                request.paymentKey(),
                request.code(),
                request.message(),
                request.amount(),
                request.rawPayload()
        );
        return PaymentFailureResponse.from(paymentFailurePersistencePort.save(failure));
    }

    private static LocalDateTime toKst(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, KST);
    }
}
