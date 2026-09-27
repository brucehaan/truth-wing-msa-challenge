package com.example.demo.payment.domain;

/**
 * 결제 승인 포트.
 * 결제 도메인은 "이 결제를 승인해 달라"와 그 결과({@link PaymentApproval})만 안다.
 * 토스의 JSON 모양, 인증 헤더, 상태 코드, 한글 결제수단 표기는 이 포트 뒤(ACL)에 숨는다 —
 * PG 를 바꾸거나 PG 가 응답 형식을 바꿔도 도메인과 애플리케이션 코드는 바뀌지 않는다.
 */
public interface PaymentGateway {

    /**
     * @throws PaymentGatewayException 승인되지 않았거나 응답이 요청과 맞지 않을 때
     */
    PaymentApproval approve(String paymentKey, String orderNo, long amount);
}
