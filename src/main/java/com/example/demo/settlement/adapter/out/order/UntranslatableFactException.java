package com.example.demo.settlement.adapter.out.order;

/**
 * 상류의 사실을 정산의 언어로 옮길 수 없다(원 단위가 아닌 금액, 판매자 없음 등).
 * 그날 수집 전체를 멈춘다 — 한 건을 조용히 빼면 통제 합계가 어긋나 결국 마감이 막히므로, 원인을 먼저 드러내는 편이 낫다.
 */
public class UntranslatableFactException extends IllegalStateException {

    public UntranslatableFactException(String reference, String reason) {
        super("정산 사실로 번역할 수 없습니다. ref=" + reference + ", reason=" + reason);
    }
}
