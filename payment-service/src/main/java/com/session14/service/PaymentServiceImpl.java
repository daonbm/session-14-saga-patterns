package com.session14.service;

import com.session14.dto.PaymentRequest;
import com.session14.dto.PaymentResponse;
import com.session14.entity.Payment;
import com.session14.entity.PaymentStatus;
import com.session14.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[PAYMENT-SERVICE] Đang xử lý thanh toán cho đơn hàng: {}, Số tiền: {}, Phương thức: {}",
                request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Xử lý thanh toán chuẩn
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(request.getOrderId())
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "BANK_TRANSFER")
                .status(PaymentStatus.SUCCESS)
                .failureReason(null)
                .transactionDate(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("[PAYMENT-SERVICE] Thanh toán thành công cho đơn hàng: {}. Mã giao dịch: {}", request.getOrderId(), paymentId);

        return mapToResponse(savedPayment, "Thanh toán thành công qua cổng thanh toán.");
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String orderId) {
        log.info("[PAYMENT-SERVICE] [COMPENSATING ACTION] Yêu cầu hoàn tiền bù trừ cho đơn hàng: {}", orderId);

        Payment payment = paymentRepository.findFirstByOrderIdOrderByTransactionDateDesc(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán cho đơn hàng: " + orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setFailureReason("Hoàn tiền bù trừ (Compensating Transaction) do Saga hủy đơn hàng.");
            paymentRepository.save(payment);
            log.info("[PAYMENT-SERVICE] [COMPENSATING ACTION] Đã hoàn tiền thành công cho đơn hàng: {}", orderId);
            return mapToResponse(payment, "Giao dịch đã được hoàn tiền (Refunded) thành công.");
        } else {
            log.warn("[PAYMENT-SERVICE] Giao dịch không ở trạng thái SUCCESS, trạng thái hiện tại: {}", payment.getStatus());
            return mapToResponse(payment, "Giao dịch không cần hoàn tiền vì trạng thái hiện tại là: " + payment.getStatus());
        }
    }

    @Override
    public PaymentResponse getPaymentByOrderId(String orderId) {
        return paymentRepository.findFirstByOrderIdOrderByTransactionDateDesc(orderId)
                .map(p -> mapToResponse(p, null))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán cho orderId: " + orderId));
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> mapToResponse(p, null))
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .message(message != null ? message : payment.getFailureReason())
                .transactionDate(payment.getTransactionDate())
                .build();
    }
}
