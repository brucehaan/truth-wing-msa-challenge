package com.example.demo.product.application.port.out;

import com.example.demo.product.domain.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPersistencePort {
    Product save(Product product);

    Optional<Product> findById(UUID productId);

    List<Product> findAll();

    void delete(Product product);
}
