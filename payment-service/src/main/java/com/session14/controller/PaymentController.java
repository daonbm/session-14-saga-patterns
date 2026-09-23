package com.session14.controller;

import com.session14.dto.PaymentRequest;
import com.session14.dto.PaymentResponse;
import com.session14.entity.PaymentStatus;
import com.session14.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${server.port}")
    private String serverPort;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("[PAYMENT-CONTROLLER] Nhận request thanh toán cho order: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(request);
        if (response.getStatus() == PaymentStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Giao dịch bù trừ (Compensating Transaction) khi Saga cần hủy/hoàn tiền
     */
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String orderId) {
        log.info("[PAYMENT-CONTROLLER] Nhận request bù trừ hoàn tiền cho order: {}", orderId);
        PaymentResponse response = paymentService.refundPayment(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/port-info")
    public ResponseEntity<String> getPortInfo() {
        return ResponseEntity.ok("Payment Service running on port: " + serverPort);
    }
}
