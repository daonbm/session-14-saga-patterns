package com.session14.saga.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "saga_orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal amount;

    private String customerId;

    @Column(nullable = false)
    private String status;

    private Instant createdAt;
    private Instant updatedAt;
    private String lastReason;

    public Order(Long id, Long productId, int quantity, BigDecimal amount, String customerId, String status) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.customerId = customerId;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String correlationId() {
        return String.valueOf(id);
    }
}