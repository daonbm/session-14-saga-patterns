package com.session06.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private String customerName;
    private String customerEmail;
    private String customerAddress;
    private String paymentMethod;

    @Builder.Default
    private List<OrderItemRequest> items = new ArrayList<>();
}
