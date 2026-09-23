package com.session06.order_service.service;

import com.session06.order_service.dto.CreateOrderRequest;
import com.session06.order_service.dto.OrderResponse;
import com.session06.order_service.dto.ProductDto;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<ProductDto> getAvailableProducts();
    OrderResponse createOrder(CreateOrderRequest request);
    List<OrderResponse> getAllOrders();
    Optional<OrderResponse> getOrderById(String id);
}
