package com.session14.service;

import com.session14.dto.PaymentRequest;
import com.session14.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse refundPayment(String orderId);
    PaymentResponse getPaymentByOrderId(String orderId);
    List<PaymentResponse> getAllPayments();
}
