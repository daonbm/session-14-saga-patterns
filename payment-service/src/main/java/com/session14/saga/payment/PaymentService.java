package com.session14.saga.payment;

import com.session14.saga.common.EventPublisher;
import com.session14.saga.common.SagaEvent;
import com.session14.saga.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final EventPublisher publisher;

    public PaymentService(PaymentRepository paymentRepository, EventPublisher publisher) {
        this.paymentRepository = paymentRepository;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "payment-service-group")
    @Transactional
    public void listenOrderEvents(SagaEvent event) {
        log.info("Payment event received: type={} eventId={} correlationId={} orderId={} amount={}",
                event.type(), event.eventId(), event.correlationId(), event.orderId(), event.amount());
        if (!"OrderCreated".equals(event.type())) {
            log.debug("Payment event ignored: type={} correlationId={}", event.type(), event.correlationId());
            return;
        }
        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("Payment duplicate event ignored: orderId={} correlationId={}", event.orderId(), event.correlationId());
            return;
        }

        Payment payment = new Payment(event.orderId(), event.customerId(), event.amount(), "COMPLETED");
        paymentRepository.save(payment);
        SagaEvent completed = SagaEvent.of("PaymentCompleted", event.correlationId(), event.orderId(),
                event.productId(), event.quantity(), event.amount(), event.customerId(), null);
        log.info("PaymentCompleted: orderId={} correlationId={}", event.orderId(), event.correlationId());
        publisher.publish(Topics.PAYMENT_EVENTS, completed);
    }

    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "payment-service-group")
    @Transactional
    public void listenInventoryEvents(SagaEvent event) {
        if (!"InventoryFailed".equals(event.type())) {
            log.debug("Payment compensation event ignored: type={} correlationId={}", event.type(), event.correlationId());
            return;
        }
        log.warn("Payment compensation started: inventory failed orderId={} reason={}", event.orderId(), event.reason());
        paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
            if ("COMPLETED".equals(payment.getStatus())) {
                payment.setStatus("REFUNDED");
                payment.setReason(event.reason());
                log.info("REFUND completed: orderId={} correlationId={} reason={}",
                        event.orderId(), event.correlationId(), event.reason());
                SagaEvent refunded = SagaEvent.of("PaymentRefunded", event.correlationId(), event.orderId(),
                        event.productId(), event.quantity(), event.amount(), event.customerId(),
                        "Refunded after inventory failure");
                publisher.publish(Topics.PAYMENT_EVENTS, refunded);
            }
        });
    }
}
