package com.session14.saga.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, SagaEvent event) {
        log.info("Saga event publish started: topic={} type={} eventId={} correlationId={} orderId={} productId={}",
                topic, event.type(), event.eventId(), event.correlationId(), event.orderId(), event.productId());
        kafkaTemplate.send(topic, event.correlationId(), event).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Saga event publish failed: topic={} type={} eventId={} correlationId={} reason={}",
                        topic, event.type(), event.eventId(), event.correlationId(), error.getMessage(), error);
                return;
            }
            log.info("Saga event publish completed: topic={} partition={} offset={} type={} eventId={} correlationId={}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset(),
                    event.type(), event.eventId(), event.correlationId());
        });
    }
}