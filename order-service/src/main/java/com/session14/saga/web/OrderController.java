package com.session14.saga.web;

import com.session14.saga.order.Order;
import com.session14.saga.order.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record CreateOrderRequest(Long productId, Integer quantity, BigDecimal amount, String customerId) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateOrderRequest req) {
        BigDecimal amount = req.amount() != null ? req.amount() : BigDecimal.valueOf(199.99);
        String customerId = req.customerId() != null ? req.customerId() : "CUST-001";
        Long productId = req.productId() != null ? req.productId() : 1L;
        int quantity = req.quantity() != null && req.quantity() > 0 ? req.quantity() : 1;
        Order order = orderService.createOrder(productId, quantity, amount, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId", order.getId(),
                "correlationId", order.correlationId(),
                "status", order.getStatus(),
                "message", "Order PENDING"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        Order order = orderService.findById(id);
        if (order == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "correlationId", order.correlationId(),
                "productId", order.getProductId(),
                "quantity", order.getQuantity(),
                "amount", order.getAmount(),
                "customerId", order.getCustomerId() == null ? "" : order.getCustomerId(),
                "status", order.getStatus(),
                "lastReason", order.getLastReason() == null ? "" : order.getLastReason()));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(Map.of("hint", "POST /api/orders to start a Saga, then GET /api/orders/{id}"));
    }
}
