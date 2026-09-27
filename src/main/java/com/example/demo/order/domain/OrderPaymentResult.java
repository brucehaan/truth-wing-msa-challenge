package com.example.demo.order.domain;

/**
 * 결제 승인 반영 결과.
 * 예외 대신 결과값으로 돌려주는 이유: 금액 불일치·주문 없음은 다시 시도해도 결과가 같다.
 * 이벤트 소비 중에 예외를 던지면 아웃박스 릴레이가 재시도 한도까지 헛되이 반복하므로, 업무 규칙 위반은 결과로 알린다.
 */
public enum OrderPaymentResult {
    CONFIRMED,
    ALREADY_CONFIRMED,
    AMOUNT_MISMATCH,
    NOT_PAYABLE,
    ORDER_NOT_FOUND
}
