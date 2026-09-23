package com.session14.saga.common;

/** Tên Kafka topic dùng cho các event của Saga. */
public final class Topics {
    public static final String ORDER_EVENTS = "order-events";
    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String INVENTORY_EVENTS = "inventory-events";

    private Topics() {
    }
}