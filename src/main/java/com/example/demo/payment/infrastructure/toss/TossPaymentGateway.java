package com.example.demo.payment.infrastructure.toss;

import com.example.demo.payment.domain.PaymentApproval;
import com.example.demo.payment.domain.PaymentGateway;
import com.example.demo.payment.domain.PaymentGatewayException;
import com.example.demo.payment.domain.PaymentGatewayException.Reason;
import com.example.demo.payment.infrastructure.toss.dto.TossPaymentResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * 토스페이먼츠 ACL — {@link PaymentGateway} 포트의 구현.
 * HTTP 호출(TossPaymentClient)과 번역(TossPaymentTranslator)을 묶고, 토스의 HTTP 오류를 도메인 예외로 바꾼다.
 * 1주차 전략 설계에서 "외부 PG 와의 관계는 Conformist"라고 적은 부분을, 코드에서는 이 계층이 경계로 격리한다.
 */
@Component
@Profile("!fake-pg")
public class TossPaymentGateway implements PaymentGateway {

    private final TossPaymentClient client;
    private final TossPaymentTranslator translator = new TossPaymentTranslator();

    public TossPaymentGateway(TossPaymentClient client) {
        this.client = client;
    }

    @Override
    public PaymentApproval approve(String paymentKey, String orderNo, long amount) {
        TossPaymentResponse response;
        try {
            response = client.confirm(paymentKey, orderNo, amount);
        } catch (RestClientResponseException e) {
            throw new PaymentGatewayException(Reason.PG_REJECTED,
                    "토스가 승인을 거절했습니다. HTTP " + e.getStatusCode().value() + " " + e.getResponseBodyAsString());
        }
        return translator.toApproval(response, orderNo, amount);
    }
}
