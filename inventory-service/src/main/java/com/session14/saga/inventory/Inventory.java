package com.session14.saga.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saga_inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    private Long productId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int availableQuantity;

    public Inventory(Long productId, String name, int availableQuantity) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
    }
}