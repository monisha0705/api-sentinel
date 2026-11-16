package com.apisentinel.bank.controller;

import com.apisentinel.bank.dto.PaymentRequest;
import com.apisentinel.bank.dto.PaymentResponse;
import com.apisentinel.bank.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {

        PaymentResponse response =
                paymentService.createPayment(request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
public ResponseEntity<PaymentResponse> getPaymentById(
        @PathVariable java.util.UUID id,
        Authentication authentication) {

    return ResponseEntity.ok(
            paymentService.getPaymentById(id, authentication)
    );
}

@PostMapping("/{id}/approve")
public ResponseEntity<PaymentResponse> approvePayment(
        @PathVariable java.util.UUID id,
        Authentication authentication) {

    return ResponseEntity.ok(
            paymentService.approvePayment(id, authentication)
    );
}

@PostMapping("/{id}/cancel")
public ResponseEntity<PaymentResponse> cancelPayment(
        @PathVariable java.util.UUID id,
        Authentication authentication) {

    return ResponseEntity.ok(
            paymentService.cancelPayment(id, authentication)
    );
}

}