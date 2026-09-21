package com.example.demo.product.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotNull
        @Schema(description = "판매자 ID")
        String sellerId,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "상품명")
        String name,

        @Schema(description = "상품 설명")
        String description,

        @NotNull
        @DecimalMin(value = "0.0")
        @Schema(description = "가격")
        BigDecimal price,

        @NotNull
        @Min(0)
        @Schema(description = "재고")
        Integer stock,

        @NotBlank
        @Size(max = 20)
        @Schema(description = "상태")
        String status,

        String creatorId
) {
}
