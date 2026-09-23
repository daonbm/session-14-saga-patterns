package com.session06.order_service.saga;

/**
 * Các trạng thái trong State Machine của Order Saga Orchestrator
 */
public enum SagaState {
    STARTED,             // Bắt đầu chuỗi Saga
    PENDING_ORDER,       // Bước 1: Đã lưu đơn hàng PENDING
    RESERVING_STOCK,     // Bước 2: Đang gửi lệnh giữ tồn kho
    STOCK_RESERVED,      // Bước 2 OK: Kho đã được giữ thành công
    PROCESSING_PAYMENT,  // Bước 3: Đang gửi lệnh thanh toán
    PAYMENT_SUCCESS,     // Bước 3 OK: Thanh toán thành công
    COMPENSATING_STOCK,  // Giao dịch bù trừ C2: Đang gọi hoàn trả lại kho
    STOCK_RELEASED,      // Giao dịch bù trừ C2 OK: Kho đã được phục hồi
    SAGA_COMPLETED,      // Trạng thái cuối: Saga thành công trọn vẹn (CONFIRMED)
    SAGA_ROLLED_BACK,    // Trạng thái cuối: Saga rollback hoàn tất (CANCELLED)
    SAGA_FAILED          // Thất bại ngay từ đầu (Kho không đủ hàng)
}
