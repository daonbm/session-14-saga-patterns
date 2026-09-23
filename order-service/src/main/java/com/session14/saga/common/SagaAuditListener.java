package com.session14.saga.common;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SagaAuditListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SagaAuditListener.class);

    @KafkaListener(topics = {Topics.ORDER_EVENTS, Topics.PAYMENT_EVENTS, Topics.INVENTORY_EVENTS},
            groupId = "saga-audit-group")
    public void audit(SagaEvent event) {
        log.info("[AUDIT] topic-event={} correlationId={} orderId={}",
                event.type(), event.correlationId(), event.orderId());
    }
}