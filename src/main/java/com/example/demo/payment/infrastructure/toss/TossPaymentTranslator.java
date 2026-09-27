package com.example.demo.payment.infrastructure.toss;

import com.example.demo.payment.domain.PaymentApproval;
import com.example.demo.payment.domain.PaymentGatewayException;
import com.example.demo.payment.domain.PaymentGatewayException.Reason;
import com.example.demo.payment.domain.PaymentMethod;
import com.example.demo.payment.infrastructure.toss.dto.TossPaymentResponse;

import java.util.Map;

/**
 * ACL 의 번역기 — 토스의 언어를 결제 도메인의 언어로 바꾼다. 스프링에 의존하지 않는 순수 클래스라 단위 테스트로 검증한다.
 *
 * <p>번역 규칙(근거: 토스페이먼츠 코어 API 문서의 Payment 객체)</p>
 * <ul>
 *   <li>status 는 "DONE"(승인됨)만 승인으로 인정한다. WAITING_FOR_DEPOSIT(가상계좌 입금 대기) 등은 승인이 아니다</li>
 *   <li>응답의 orderId·totalAmount 가 우리가 요청한 값과 다르면 거부한다 — 다른 결제의 승인 결과를 받아들이지 않는다</li>
 *   <li>method 의 한글 표기를 {@link PaymentMethod} 로 바꾼다. 모르는 값은 UNKNOWN (승인은 막지 않는다)</li>
 *   <li>approvedAt 은 오프셋(+09:00)을 보존해 Instant 로 바꾼다 — toLocalDateTime() 처럼 오프셋을 버리지 않는다</li>
 * </ul>
 */
public final class TossPaymentTranslator {

    static final String APPROVED = "DONE";

    private static final Map<String, PaymentMethod> METHODS = Map.of(
            "카드", PaymentMethod.CARD,
            "가상계좌", PaymentMethod.VIRTUAL_ACCOUNT,
            "간편결제", PaymentMethod.EASY_PAY,
            "휴대폰", PaymentMethod.MOBILE_PHONE,
            "계좌이체", PaymentMethod.TRANSFER,
            "문화상품권", PaymentMethod.CULTURE_GIFT_CERTIFICATE,
            "도서문화상품권", PaymentMethod.BOOK_GIFT_CERTIFICATE,
            "게임문화상품권", PaymentMethod.GAME_GIFT_CERTIFICATE);

    public PaymentApproval toApproval(TossPaymentResponse response, String requestedOrderNo, long requestedAmount) {
        if (response == null) {
            throw new PaymentGatewayException(Reason.EMPTY_RESPONSE, "Toss 결제 승인 응답이 비어 있습니다.");
        }
        if (!APPROVED.equals(response.status())) {
            throw new PaymentGatewayException(Reason.NOT_APPROVED,
                    "승인되지 않은 결제입니다. status=" + response.status() + ", orderId=" + response.orderId());
        }
        if (!requestedOrderNo.equals(response.orderId())) {
            throw new PaymentGatewayException(Reason.ORDER_MISMATCH,
                    "요청한 주문과 승인 응답의 주문이 다릅니다. requested=" + requestedOrderNo + ", response=" + response.orderId());
        }
        if (response.totalAmount() == null || response.totalAmount() != requestedAmount) {
            throw new PaymentGatewayException(Reason.AMOUNT_MISMATCH,
                    "요청 금액과 승인 금액이 다릅니다. requested=" + requestedAmount + ", response=" + response.totalAmount());
        }
        if (response.approvedAt() == null) {
            throw new PaymentGatewayException(Reason.MISSING_APPROVED_AT, "승인 시각이 없습니다. orderId=" + response.orderId());
        }
        return new PaymentApproval(
                response.paymentKey(),
                response.orderId(),
                response.totalAmount(),
                methodOf(response.method()),
                response.approvedAt().toInstant(),
                response.requestedAt() == null ? null : response.requestedAt().toInstant());
    }

    static PaymentMethod methodOf(String tossMethod) {
        if (tossMethod == null) {
            return PaymentMethod.UNKNOWN;
        }
        return METHODS.getOrDefault(tossMethod, PaymentMethod.UNKNOWN);
    }
}
