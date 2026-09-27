package com.example.demo.settlement.adapter.out.memory;

import com.example.demo.settlement.application.port.out.FeePolicyPort;
import com.example.demo.settlement.domain.fee.FeePolicy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryFeePolicies implements FeePolicyPort {

    private final List<FeePolicy> policies = new ArrayList<>();

    public InMemoryFeePolicies add(FeePolicy policy) {
        policies.add(policy);
        return this;
    }

    @Override
    public List<FeePolicy> candidates(String sellerId, LocalDate on) {
        return policies.stream()
                .filter(p -> p.sellerId() == null || p.sellerId().equals(sellerId))
                .filter(p -> p.isEffectiveOn(on))
                .toList();
    }

    @Override
    public FeePolicy getById(UUID policyId) {
        return policies.stream().filter(p -> p.id().equals(policyId)).findFirst().orElseThrow();
    }
}
