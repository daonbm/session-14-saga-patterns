package com.session14.saga.common;

import java.math.BigDecimal;
import java.util.UUID;

public record SagaEvent(
        String eventId,
        String correlationId,
        String type,
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal amount,
        String customerId,
        String reason) {

    public static SagaEvent of(String type, SagaEvent source) {
        return new SagaEvent(UUID.randomUUID().toString(), source.correlationId(), type, source.orderId(),
                source.productId(), source.quantity(), source.amount(), source.customerId(), source.reason());
    }

    public static SagaEvent of(String type, String correlationId, Long orderId, Long productId, int quantity,
                               BigDecimal amount, String customerId, String reason) {
        return new SagaEvent(UUID.randomUUID().toString(), correlationId, type, orderId, productId, quantity,
                amount, customerId, reason);
    }

    public SagaEvent withReason(String newReason) {
        return new SagaEvent(UUID.randomUUID().toString(), correlationId, type, orderId, productId, quantity,
                amount, customerId, newReason);
    }
}
