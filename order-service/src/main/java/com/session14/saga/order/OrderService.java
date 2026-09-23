package com.session14.saga.order;

import com.session14.saga.common.EventPublisher;
import com.session14.saga.common.SagaEvent;
import com.session14.saga.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final EventPublisher publisher;
    private final AtomicLong idSeq = new AtomicLong(System.currentTimeMillis());

    public OrderService(OrderRepository orderRepository, EventPublisher publisher) {
        this.orderRepository = orderRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Order createOrder(Long productId, int quantity, BigDecimal amount, String customerId) {
        log.info("Saga start: productId={} quantity={} amount={} customerId={}", productId, quantity, amount, customerId);
        Long orderId = idSeq.incrementAndGet();
        Order order = new Order(orderId, productId, quantity, amount, customerId, "PENDING");
        orderRepository.save(order);
        SagaEvent event = SagaEvent.of("OrderCreated", order.correlationId(), orderId, productId, quantity,
                amount, customerId, null);
        log.info("OrderService: OrderCreated orderId={} correlationId={} amount={}", orderId, order.correlationId(), amount);
        publisher.publish(Topics.ORDER_EVENTS, event);
        return order;
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "order-service-group")
    @Transactional
    public void listenInventory(SagaEvent event) {
        switch (event.type()) {
            case "InventoryDeducted" -> updateStatus(event, "CONFIRMED", null);
            case "InventoryFailed" -> updateStatus(event, "CANCELLED", event.reason());
            default -> log.debug("Order event ignored: type={} correlationId={}", event.type(), event.correlationId());
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "order-service-group")
    @Transactional
    public void listenPayment(SagaEvent event) {
        if ("PaymentFailed".equals(event.type())) {
            updateStatus(event, "CANCELLED", event.reason());
        } else {
            log.debug("Order payment event ignored: type={} correlationId={}", event.type(), event.correlationId());
        }
    }

    private void updateStatus(SagaEvent event, String status, String reason) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null || !"PENDING".equals(order.getStatus())) return;
        order.setStatus(status);
        order.setLastReason(reason);
        order.setUpdatedAt(Instant.now());
        log.info("OrderService: received {} -> order {} = {} reason={}", event.type(), event.orderId(), status, reason);
    }
}
