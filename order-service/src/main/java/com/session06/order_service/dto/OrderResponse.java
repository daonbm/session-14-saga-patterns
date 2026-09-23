package com.session06.order_service.dto;

import com.session06.order_service.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String orderId;
    private String customerName;
    private String customerEmail;
    private String customerAddress;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;

    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
    
    private String message;
}
