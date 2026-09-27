package com.example.demo.payment.application;

import com.example.demo.common.event.EventRecorder;
import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentApproval;
import com.example.demo.payment.domain.PaymentFailure;
import com.example.demo.payment.domain.PaymentGateway;
import com.example.demo.payment.infrastructure.PaymentFailureRepository;
import com.example.demo.payment.infrastructure.PaymentRepository;
import com.example.demo.payment.presentation.dto.PaymentConfirmRequest;
import com.example.demo.payment.presentation.dto.PaymentFailRequest;
import com.example.demo.payment.presentation.dto.PaymentFailureResponse;
import com.example.demo.payment.presentation.dto.PaymentResponse;
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
 * 2주차 변경 요약
 * <ul>
 *   <li>ACL: 토스 클라이언트를 직접 쓰지 않고 {@link PaymentGateway} 포트만 안다</li>
 *   <li>EDA: 결제 저장과 {@link PaymentApproved} 기록을 한 트랜잭션으로 묶는다(트랜잭셔널 아웃박스).
 *       주문 확정은 주문 컨텍스트가 이벤트를 받아 스스로 한다 — 결제가 주문 서비스를 호출하지 않는다</li>
 *   <li>버그 수정: 클래스 단의 {@code @Transactional(readOnly = true)} 를 쓰기 메서드가 그대로 물려받아,
 *       confirm/recordFailure 의 저장이 읽기 전용 트랜잭션에서 실행되던 문제를 없앴다</li>
 * </ul>
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PaymentRepository paymentRepository;
    private final PaymentFailureRepository paymentFailureRepository; // 질문 : 실무에서는 결제 실패 / 주문 실패처리를 어떻게 하는가?
    private final PaymentGateway paymentGateway;
    private final EventRecorder eventRecorder;
    private final TransactionTemplate tx;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentFailureRepository paymentFailureRepository,
                              PaymentGateway paymentGateway,
                              EventRecorder eventRecorder,
                              PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.paymentFailureRepository = paymentFailureRepository;
        this.paymentGateway = paymentGateway;
        this.eventRecorder = eventRecorder;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll(Pageable pageable) {
        return paymentRepository.findAll(pageable).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * 1) 이미 승인된 결제면 그대로 돌려준다 — success 페이지 새로고침으로 confirm 이 다시 불려도 안전하게(기존 동작 유지)
     * 2) PG 승인은 트랜잭션 밖에서 호출한다 — 외부 HTTP 응답을 기다리는 동안 DB 커넥션을 잡고 있지 않기 위해서다
     * 3) 결제 저장 + PaymentApproved 기록을 한 트랜잭션으로 커밋한다 — 둘 중 하나만 남는 경우가 없다
     */
    @Override
    public PaymentResponse confirm(PaymentConfirmRequest request) {
        if (request.paymentKey() == null || request.orderId() == null || request.amount() == null) {
            throw new IllegalArgumentException("paymentKey, orderId, amount 는 필수입니다.");
        }
        Optional<Payment> alreadyConfirmed = paymentRepository.findByPaymentKey(request.paymentKey());
        if (alreadyConfirmed.isPresent()) {
            return PaymentResponse.from(alreadyConfirmed.get());
        }

        PaymentApproval approval = paymentGateway.approve(request.paymentKey(), request.orderId(), request.amount());

        Payment saved = tx.execute(status -> {
            Optional<Payment> raced = paymentRepository.findByPaymentKey(approval.paymentKey());
            if (raced.isPresent()) {
                return raced.get();                                   // 같은 승인이 동시에 두 번 — 먼저 저장된 쪽을 돌려준다
            }
            Payment payment = Payment.create(approval.paymentKey(), approval.orderNo(), approval.amount());
            payment.markConfirmed(approval.method().name(), toKst(approval.approvedAt()), toKst(approval.requestedAt()));
            Payment persisted = paymentRepository.save(payment);      // merge 경로 — 반환값을 쓴다
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
        return PaymentFailureResponse.from(paymentFailureRepository.save(failure));
    }

    private static LocalDateTime toKst(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, KST);
    }
}
