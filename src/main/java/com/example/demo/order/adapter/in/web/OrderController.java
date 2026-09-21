package com.example.demo.order.adapter.in.web;

import com.example.demo.order.adapter.in.web.dto.OrderCreateRequest;
import com.example.demo.order.adapter.in.web.dto.OrderResponse;
import com.example.demo.order.application.port.in.OrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "주문 정보 API")
public class OrderController {
    private final OrderUseCase orderUseCase;

    @PostMapping
    @Operation(summary = "주문 생성", description = "정산 대상 주문 정보를 생성합니다.")
    public ResponseEntity<OrderResponse> create(@RequestBody OrderCreateRequest request) { // 질문 : ResponseEntity는 실무에서 자주 쓰나?
        return ResponseEntity.status(CREATED).body(orderUseCase.create(request));
    }

    @GetMapping
    @Operation(summary = "주문 전체 조회", description = "현재 저장된 주문 정보를 모두 조회합니다.")
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderUseCase.getAll());
    }


}
