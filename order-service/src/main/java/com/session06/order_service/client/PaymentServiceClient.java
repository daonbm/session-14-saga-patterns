package com.session06.order_service.client;

import com.session06.order_service.dto.PaymentRequestDto;
import com.session06.order_service.dto.PaymentResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.payment-service.url:http://payment-service}")
    private String paymentServiceUrl;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        String url = paymentServiceUrl + "/api/payments/process";
        log.info("[SAGA ORCHESTRATOR -> PAYMENT] Gửi yêu cầu thanh toán tới: {}, Order: {}, Amount: {}",
                url, request.getOrderId(), request.getAmount());
        try {
            HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(request);
            ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.BadRequest e) {
            log.warn("[SAGA ORCHESTRATOR -> PAYMENT] Payment Service từ chối thanh toán: {}", e.getResponseBodyAsString());
            return PaymentResponseDto.builder()
                    .orderId(request.getOrderId())
                    .status("FAILED")
                    .message("Thanh toán bị từ chối: " + e.getResponseBodyAsString())
                    .build();
        } catch (Exception e) {
            log.error("[SAGA ORCHESTRATOR -> PAYMENT] Lỗi giao tiếp với Payment Service: {}", e.getMessage());
            throw new RuntimeException("Lỗi giao tiếp với Payment Service: " + e.getMessage(), e);
        }
    }

    /**
     * Giao dịch bù trừ (Compensating Transaction) khi cần hoàn tiền
     */
    public PaymentResponseDto refundPayment(String orderId) {
        String url = paymentServiceUrl + "/api/payments/refund/" + orderId;
        log.info("[SAGA ORCHESTRATOR -> PAYMENT] [COMPENSATING ACTION] Gửi yêu cầu hoàn tiền cho đơn hàng: {}", orderId);
        try {
            ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    PaymentResponseDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("[SAGA ORCHESTRATOR -> PAYMENT] Lỗi khi hoàn tiền đơn hàng {}: {}", orderId, e.getMessage());
            return PaymentResponseDto.builder()
                    .orderId(orderId)
                    .status("REFUND_FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }

    public PaymentResponseDto processPaymentFallback(PaymentRequestDto request, Throwable t) {
        log.warn("[CIRCUIT BREAKER] Fallback kích hoạt cho processPayment. Service không khả dụng. Lý do: {}", t.getMessage());
        return PaymentResponseDto.builder()
                .orderId(request.getOrderId())
                .status("FAILED")
                .message("Payment Service tạm thời gián đoạn (Circuit Breaker kích hoạt): " + t.getMessage())
                .build();
    }
}
