package com.session14.saga.inventory;

import com.session14.saga.common.EventPublisher;
import com.session14.saga.common.SagaEvent;
import com.session14.saga.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final EventPublisher publisher;

    public InventoryService(InventoryRepository inventoryRepository, EventPublisher publisher) {
        this.inventoryRepository = inventoryRepository;
        this.publisher = publisher;
    }

    @Override
    public void run(String... args) {
        if (inventoryRepository.count() == 0) {
            inventoryRepository.saveAll(List.of(
                    new Inventory(1L, "Laptop Dell XPS 15", 15),
                    new Inventory(2L, "iPhone 16 Pro Max 256GB", 30),
                    new Inventory(3L, "Sony WH-1000XM5", 40),
                    new Inventory(4L, "Keychron K2", 0)));
            log.info("Inventory initialized: productId=4 starts with zero stock for out-of-stock demo");
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "inventory-service-group")
    @Transactional
    public void listenPaymentEvents(SagaEvent event) {
        log.info("Inventory event received: type={} eventId={} correlationId={} orderId={} productId={} quantity={}",
                event.type(), event.eventId(), event.correlationId(), event.orderId(), event.productId(), event.quantity());
        if (!"PaymentCompleted".equals(event.type())) {
            log.debug("Inventory event ignored: type={} correlationId={}", event.type(), event.correlationId());
            return;
        }

        Inventory item = inventoryRepository.findById(event.productId()).orElse(null);
        boolean failed = item == null || item.getAvailableQuantity() < event.quantity();

        if (failed) {
            String reason = item == null ? "Product not found: " + event.productId()
                    : "Out of stock: only " + item.getAvailableQuantity() + " left";
            publisher.publish(Topics.INVENTORY_EVENTS, SagaEvent.of("InventoryFailed", event.correlationId(),
                    event.orderId(), event.productId(), event.quantity(), event.amount(), event.customerId(), reason));
            log.info("InventoryService: InventoryFailed orderId={} reason={}", event.orderId(), reason);
            return;
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - event.quantity());
        publisher.publish(Topics.INVENTORY_EVENTS, SagaEvent.of("InventoryDeducted", event.correlationId(),
                event.orderId(), event.productId(), event.quantity(), event.amount(), event.customerId(),
                String.valueOf(item.getAvailableQuantity())));
        log.info("InventoryService: InventoryDeducted orderId={} remaining={}", event.orderId(), item.getAvailableQuantity());
    }

    @Transactional(readOnly = true)
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }
}