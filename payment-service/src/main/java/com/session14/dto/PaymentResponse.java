package com.session14.dto;

import com.session14.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String message;
    private LocalDateTime transactionDate;
}
