package com.session14.saga.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "saga_payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    private String customerId;
    private BigDecimal amount;

//    COMPLETED | FAILED | REFUNDED
    @Column(nullable = false)
    private String status;

    private Instant createdAt = Instant.now();
    private String reason;

    public Payment(Long orderId, String customerId, BigDecimal amount, String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }
}