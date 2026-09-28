package com.example.demo.payment.adapter.out.pg.fake;

import com.example.demo.payment.domain.PaymentApproval;
import com.example.demo.payment.application.port.out.PaymentGateway;
import com.example.demo.payment.domain.PaymentMethod;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * 로컬 시연용 가짜 PG — {@code fake-pg} 프로파일에서만 켜진다.
 * 토스 테스트 키 없이 "결제 승인 → PaymentApproved → 주문 결제 확정" 이벤트 흐름을 확인하기 위한 것이다.
 * 포트 덕분에 애플리케이션 코드는 진짜/가짜 PG 를 구분하지 않는다.
 */
@Component
@Profile("fake-pg")
public class FakePaymentGateway implements PaymentGateway {

    private final Clock clock;

    public FakePaymentGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PaymentApproval approve(String paymentKey, String orderNo, long amount) {
        Instant now = clock.instant();
        return new PaymentApproval(paymentKey, orderNo, amount, PaymentMethod.CARD, now, now);
    }
}
