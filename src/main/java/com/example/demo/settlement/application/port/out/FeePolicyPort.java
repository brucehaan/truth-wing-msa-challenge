package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.fee.FeePolicy;

import java.time.LocalDate;
import java.util.List;

public interface FeePolicyPort {
    /** sellerId 전용 정책 + 기본 정책 중 on 날짜에 유효한 후보. 선택은 도메인(FeePolicySelector)이 한다. */
    List<FeePolicy> candidates(String sellerId, LocalDate on);

    FeePolicy getById(java.util.UUID policyId);
}
