package com.example.demo.payment.application.port.out;

import com.example.demo.payment.domain.PaymentApproval;

/**
 * PG 승인 포트(출력 포트). 결제는 "승인해 달라"와 그 결과(PaymentApproval)만 안다.
 * 토스의 JSON 모양·인증 헤더·상태 코드·한글 결제수단 표기는 이 포트 뒤의 ACL(adapter.out.pg.toss)에 갇힌다.
 * 승인되지 않았거나 응답이 요청과 맞지 않으면 PaymentGatewayException(domain)을 던진다.
 */
public interface PaymentGateway {

    PaymentApproval approve(String paymentKey, String orderNo, long amount);
}
