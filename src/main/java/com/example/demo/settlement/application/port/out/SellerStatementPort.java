package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.closing.SellerStatement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SellerStatementPort {
    Optional<SellerStatement> find(String sellerId, LocalDate businessDate);
    void save(SellerStatement statement);
    List<SellerStatement> findByDate(LocalDate businessDate);
}
