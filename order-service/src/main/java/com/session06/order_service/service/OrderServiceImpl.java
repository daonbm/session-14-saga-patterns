package com.session06.order_service.service;

import com.session06.order_service.client.ProductServiceClient;
import com.session06.order_service.dto.*;
import com.session06.order_service.model.Order;
import com.session06.order_service.model.OrderItem;
import com.session06.order_service.model.OrderStatus;
import com.session06.order_service.repository.OrderRepository;
import com.session06.order_service.saga.OrderSagaContext;
import com.session06.order_service.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final ProductServiceClient productServiceClient;
    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    @Override
    public List<ProductDto> getAvailableProducts() {
        log.info("Fetching available products catalog from Product Service...");
        return productServiceClient.getAllProducts();
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Processing new order creation for customer: {}", request.getCustomerName());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất 1 sản phẩm.");
        }

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<OrderItem> orderItems = new ArrayList<>();
        List<StockReservationItem> reservationItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .id(orderId)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerAddress(request.getCustomerAddress())
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductDto product = productServiceClient.getProductById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + itemRequest.getProductId()));

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(subTotal);

            reservationItems.add(StockReservationItem.builder()
                    .productId(product.getId())
                    .quantity(itemRequest.getQuantity())
                    .build());

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemRequest.getQuantity())
                    .subTotal(subTotal)
                    .order(order)
                    .build());
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // ĐÓNG GÓI NGỮ CẢNH VÀ GIAO CHO SAGA ORCHESTRATOR ĐIỀU PHỐI THEO STATE MACHINE
        OrderSagaContext context = OrderSagaContext.builder()
                .order(order)
                .reservationItems(reservationItems)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .build();

        boolean sagaSuccess = orderSagaOrchestrator.executeOrderSaga(context);

        String message = sagaSuccess
                ? "Đặt hàng và thanh toán thành công qua State Machine Saga Orchestration."
                : "Đơn hàng bị hủy do: " + context.getFailureMessage() + ". Hệ thống đã tự động thực thi giao dịch bù trừ (Compensating Transaction) hoàn lại tồn kho thành công!";

        return mapToOrderResponse(order, message);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders from MySQL database...");
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(order -> mapToOrderResponse(order, null))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrderResponse> getOrderById(String id) {
        log.info("Fetching order by ID: {} from MySQL database...", id);
        return orderRepository.findById(id)
                .map(order -> mapToOrderResponse(order, null));
    }

    private OrderResponse mapToOrderResponse(Order order, String customMessage) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subTotal(item.getSubTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerAddress(order.getCustomerAddress())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .message(customMessage)
                .build();
    }
}
