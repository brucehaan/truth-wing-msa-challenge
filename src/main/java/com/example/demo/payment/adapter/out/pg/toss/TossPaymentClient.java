package com.example.demo.payment.adapter.out.pg.toss;

import com.example.demo.payment.adapter.out.pg.toss.dto.TossPaymentResponse;
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

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * 토스 결제 승인 API 호출(HTTP 만 담당). 응답 해석과 번역은 {@link TossPaymentTranslator} 가 한다.
 * 2주차 변경: 프레젠테이션 DTO(PaymentConfirmRequest) 대신 원시 값을 받는다 — 인프라가 웹 계층 타입에 의존하지 않게.
 */
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestClient restClient;
    private final TossPaymentProperties tossPaymentProperties;

    public TossPaymentClient(RestClient restClient, TossPaymentProperties tossPaymentProperties) {
        this.restClient = restClient;
        this.tossPaymentProperties = tossPaymentProperties;
    }

    /**
     * 토스 결제 승인 API 호출.
     * secret key 는 호출 시점에 검증한다 — 프로퍼티가 없어도 애플리케이션 기동은 되게 하기 위해서다.
     */
    public TossPaymentResponse confirm(String paymentKey, String orderId, long amount) {
        String secretKey = tossPaymentProperties.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "Toss secret key is not configured. payment.toss.secret-key 를 설정하세요.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", paymentKey);
        body.put("orderId", orderId);
        body.put("amount", amount);

        HttpHeaders headers = createHeaders(secretKey);
        return restClient.post()
                .uri(URI.create(CONFIRM_URL))
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(body)
                .retrieve()
                .body(TossPaymentResponse.class);
    }

    /** secret key 를 "secretKey:" 형태로 Base64 인코딩해 Basic 인증으로 보낸다. */
    private HttpHeaders createHeaders(String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return headers;
    }
}
