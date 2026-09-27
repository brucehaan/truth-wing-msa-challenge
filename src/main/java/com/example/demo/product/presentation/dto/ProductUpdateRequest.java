package com.example.demo.product.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.ToString;

import java.math.BigDecimal;

@Schema(description = "상품 수정 요청")
public record ProductUpdateRequest(
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

        String modifierId
) {
}
