package com.example.demo.payment.domain;

/**
 * 결제 도메인의 결제수단. PG 사가 쓰는 표기(토스는 "카드", "간편결제" 같은 한글 문자열)와 분리된 우리 언어다.
 * PG 표기 → 이 enum 번역은 ACL(infrastructure.toss.TossPaymentTranslator)이 맡는다.
 */
public enum PaymentMethod {
    CARD,
    VIRTUAL_ACCOUNT,
    EASY_PAY,
    MOBILE_PHONE,
    TRANSFER,
    CULTURE_GIFT_CERTIFICATE,
    BOOK_GIFT_CERTIFICATE,
    GAME_GIFT_CERTIFICATE,
    /** PG 가 새 결제수단을 추가했거나 값을 주지 않은 경우. 승인 자체를 막지는 않는다. */
    UNKNOWN
}
