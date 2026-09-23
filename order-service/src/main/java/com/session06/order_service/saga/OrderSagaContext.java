package com.session06.order_service.saga;

import com.session06.order_service.dto.StockReservationItem;
import com.session06.order_service.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSagaContext {
    private Order order;
    private List<StockReservationItem> reservationItems;
    private BigDecimal totalAmount;
    private String paymentMethod;

    private SagaState currentState;
    private String failureMessage;
    private String paymentTransactionId;
}
