package com.example.demo.product.application.port.out;

import com.example.demo.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 상품 저장 포트 — Spring Data 리포지토리 인터페이스를 출력 포트로 쓴다(구현체는 Spring Data 가 만든다). */
public interface ProductPersistencePort extends JpaRepository<Product, UUID> {
}
