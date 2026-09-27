package com.example.demo.config.exception;

import com.example.demo.payment.domain.PaymentGatewayException;
import com.example.demo.settlement.application.service.IncompleteSourceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? "Validation failed" : fieldError.getDefaultMessage();
        return build(BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = resolveBadRequestMessage(ex);
        return build(BAD_REQUEST, message, request.getRequestURI());
    }

    /** PG 가 승인하지 않았거나 응답이 요청과 맞지 않음 — 상류(PG) 문제로 본다. */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGateway(PaymentGatewayException ex, HttpServletRequest request) {
        return build(BAD_GATEWAY, "[" + ex.reason() + "] " + ex.getMessage(), request.getRequestURI());
    }

    /**
     * 정산 규칙 위반 — 통제 합계 불일치(완결성 게이트), 이미 마감됨, 시산표 불일치 등.
     * 요청 형식은 맞지만 현재 상태와 충돌하므로 409 로 돌려준다.
     */
    @ExceptionHandler({IncompleteSourceException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return build(CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    private String resolveBadRequestMessage(Exception ex) {
        if (ex instanceof MissingRequestHeaderException missing) {
            return "Missing required header: " + missing.getHeaderName();
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter: " + mismatch.getName();
        }
        return ex.getMessage();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }
}
