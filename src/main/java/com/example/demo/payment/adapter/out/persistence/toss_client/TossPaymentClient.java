package com.example.demo.payment.adapter.out.persistence.toss_client;

import com.example.demo.payment.adapter.out.persistence.toss_client.dto.TossPaymentResponse;
import com.example.demo.payment.adapter.in.web.dto.PaymentConfirmRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {
    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestClient restClient;
    private final TossPaymentProperties tossPaymentProperties;

    /**
     * 토스 결제 승인 API 호출
     * 기존 @Value("${payment.toss.secret-key}") 주입은 프로퍼티가 없으면 애플리케이션 기동 자체가 실패하므로,
     * TossPaymentProperties를 통해 받아 호출 시점에 검증하도록 바꿨다.
     */
    public TossPaymentResponse confirm(PaymentConfirmRequest request) {
        String secretKey = tossPaymentProperties.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Toss secret key is not configured. payment.toss.secret-key 를 설정하세요."
            );
        }

        HttpHeaders headers = createHeaders(secretKey);

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.paymentKey());
        body.put("orderId", request.orderId());
        body.put("amount", request.amount());

        return restClient.post()
                .uri(URI.create(CONFIRM_URL))
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(body)
                .retrieve()
                .body(TossPaymentResponse.class);
    }

    /**
     * 토스에 전달하는 헤더값. secret key를 "secretKey:"형태로 Base64 인코딩해 Basic 인증으로 보낸다.
     * @param secretKey
     * @return
     */
    private HttpHeaders createHeaders(String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String auth = secretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return headers;
    }

}
