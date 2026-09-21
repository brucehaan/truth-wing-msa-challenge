package com.example.demo.payment.adapter.in.web;

import com.example.demo.payment.adapter.in.web.dto.PaymentConfirmRequest;
import com.example.demo.payment.adapter.in.web.dto.PaymentFailRequest;
import com.example.demo.payment.adapter.in.web.dto.PaymentFailureResponse;
import com.example.demo.payment.adapter.in.web.dto.PaymentResponse;
import com.example.demo.payment.application.port.in.PaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "토스 결제 API")
public class PaymentController {
    private final PaymentUseCase paymentUseCase;

    @GetMapping
    @Operation(summary = "결제 내역 조회", description = "확정된 결제 정보를 페이지 단위로 조회한다.")
    public ResponseEntity<List<PaymentResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(paymentUseCase.getAll(pageable));
    }

    @PostMapping("/confirm")
    @Operation(summary = "토스 결제 승인", description = "토스 결제 완료 후 paymentKey/orderId/amount를 전달받아 결제를 승인한다.")
    public ResponseEntity<PaymentResponse> confirm(@RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.status(CREATED).body(paymentUseCase.confirm(request));
    }

    @PostMapping("/fail")
    @Operation(summary = "결제 실패 기록", description = "토스 결제 실패 정보를 저장한다.")
    public ResponseEntity<PaymentFailureResponse> fail(@RequestBody PaymentFailRequest request) {
        return ResponseEntity.status(CREATED).body(paymentUseCase.recordFailure(request));
    }

}
