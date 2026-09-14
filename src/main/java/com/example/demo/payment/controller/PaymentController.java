package com.example.demo.payment.controller;

import com.example.demo.payment.dto.PaymentConfirmRequest;
import com.example.demo.payment.dto.PaymentFailRequest;
import com.example.demo.payment.dto.PaymentFailureResponse;
import com.example.demo.payment.dto.PaymentResponse;
import com.example.demo.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "토스 결제 API")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "결제 내역 조회", description = "확정된 결제 정보를 페이지 단위로 조회한다.")
    public ResponseEntity<List<PaymentResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAll(pageable));
    }

    @PostMapping("/confirm")
    @Operation(summary = "토스 결제 승인", description = "토스 결제 완료 후 paymentKey/orderId/amount를 전달받아 결제를 승인한다.")
    public ResponseEntity<PaymentResponse> confirm(@RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.status(CREATED).body(paymentService.confirm(request));
    }

    @PostMapping("/fail")
    @Operation(summary = "결제 실패 기록", description = "토스 결제 실패 정보를 저장한다.")
    public ResponseEntity<PaymentFailureResponse> fail(@RequestBody PaymentFailRequest request) {
        return ResponseEntity.status(CREATED).body(paymentService.recordFailure(request));
    }

}
