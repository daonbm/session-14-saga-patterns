package com.session06.order_service.saga;

import com.session06.order_service.client.PaymentServiceClient;
import com.session06.order_service.client.ProductServiceClient;
import com.session06.order_service.dto.PaymentRequestDto;
import com.session06.order_service.dto.PaymentResponseDto;
import com.session06.order_service.model.Order;
import com.session06.order_service.model.OrderStatus;
import com.session06.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * SAGA ORCHESTRATOR — BỘ ĐIỀU PHỐI THEO MÔ HÌNH STATE MACHINE (CỖ MÁY TRẠNG THÁI)
 * =========================================================================
 * Đóng vai trò là Nhạc trưởng trung tâm điều phối toàn bộ vòng đời của giao dịch phân tán:
 * - Quản lý các bước chuyển tiếp trạng thái (State Transitions).
 * - Phát lệnh tuần tự tới các Worker (Product Service, Payment Service).
 * - Tự động phát hiện sự cố và điều phối giao dịch bù trừ (Compensating Transactions).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final ProductServiceClient productServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderRepository orderRepository;

    /**
     * Điểm kích hoạt thực thi chuỗi giao dịch Saga theo cỗ máy trạng thái
     *
     * @param context Ngữ cảnh chứa thông tin đơn hàng và trạng thái hiện tại
     * @return true nếu Saga hoàn tất thành công, false nếu bị rollback/hủy
     */
    public boolean executeOrderSaga(OrderSagaContext context) {
        transitionTo(context, SagaState.STARTED);
        String orderId = context.getOrder().getId();

        log.info("╔════════════════════════════════════════════════════════════════════════╗");
        log.info("║ [SAGA ORCHESTRATOR - STATE MACHINE] BẮT ĐẦU ĐƠN HÀNG: {}       ║", orderId);
        log.info("╚════════════════════════════════════════════════════════════════════════╝");

        try {
            // =====================================================================
            // BƯỚC 1: LOCAL TRANSACTION 1 (Tạo đơn hàng ở trạng thái PENDING)
            // =====================================================================
            step1_CreatePendingOrder(context);

            // =====================================================================
            // BƯỚC 2: FORWARD TRANSACTION 2 (Yêu cầu Product Service giữ kho)
            // =====================================================================
            boolean stockOk = step2_ReserveStock(context);
            if (!stockOk) {
                // Thất bại tại Bước 2 (Kho hết hàng hoặc lỗi) -> Chuyển sang SAGA_FAILED
                transitionTo(context, SagaState.SAGA_FAILED);
                cancelOrder(context.getOrder(), "Tồn kho không đủ để đáp ứng đơn hàng.");
                return false;
            }

            // =====================================================================
            // BƯỚC 3: FORWARD TRANSACTION 3 (Yêu cầu Payment Service thanh toán)
            // =====================================================================
            boolean paymentOk = step3_ProcessPayment(context);
            if (paymentOk) {
                // =================================================================
                // HAPPY PATH: THANH TOÁN THÀNH CÔNG -> HOÀN TẤT ĐƠN HÀNG
                // =================================================================
                step4_ConfirmOrder(context);
                transitionTo(context, SagaState.SAGA_COMPLETED);
                log.info("╔════════════════════════════════════════════════════════════════════════╗");
                log.info("║ [SAGA ORCHESTRATOR] >>> SAGA THÀNH CÔNG (CONFIRMED) CHO ĐƠN: {} ║", orderId);
                log.info("╚════════════════════════════════════════════════════════════════════════╝");
                return true;
            } else {
                // =================================================================
                // SAD PATH: THANH TOÁN THẤT BÀI -> KÍCH HOẠT BÙ TRỪ (COMPENSATION)
                // =================================================================
                log.warn("╔════════════════════════════════════════════════════════════════════════╗");
                log.warn("║ [SAGA ORCHESTRATOR] >>> PHÁT HIỆN SỰ CỐ: THANH TOÁN THẤT BÀI!         ║");
                log.warn("║ >>> KÍCH HOẠT CHUỖI GIAO DỊCH BÙ TRỪ (COMPENSATING TRANSACTIONS) <<<   ║");
                log.warn("╚════════════════════════════════════════════════════════════════════════╝");

                // BÙ TRỪ C2: Hoàn lại tồn kho đã trừ ở Bước 2
                compensate_ReleaseStock(context);

                // BÙ TRỪ C1: Đổi trạng thái đơn hàng thành CANCELLED
                cancelOrder(context.getOrder(), context.getFailureMessage());

                transitionTo(context, SagaState.SAGA_ROLLED_BACK);
                log.info("╔════════════════════════════════════════════════════════════════════════╗");
                log.info("║ [SAGA ORCHESTRATOR] >>> SAGA ROLLBACK HOÀN TẤT - HỆ THỐNG ĐÃ NHẤT QUÁN ║");
                log.info("╚════════════════════════════════════════════════════════════════════════╝");
                return false;
            }

        } catch (Exception e) {
            log.error("[SAGA ORCHESTRATOR] Gặp ngoại lệ bất thường: {}", e.getMessage(), e);
            context.setFailureMessage("Ngoại lệ hệ thống: " + e.getMessage());
            compensate_ReleaseStock(context);
            cancelOrder(context.getOrder(), context.getFailureMessage());
            transitionTo(context, SagaState.SAGA_ROLLED_BACK);
            return false;
        }
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ TỪNG BƯỚC (STEP ACTIONS)
    // =========================================================================

    private void step1_CreatePendingOrder(OrderSagaContext context) {
        Order order = context.getOrder();
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        transitionTo(context, SagaState.PENDING_ORDER);
        log.info("[SAGA ORCHESTRATOR] [STEP 1 OK] Đã lưu Order {} (Status: PENDING), Tổng tiền: {}",
                order.getId(), context.getTotalAmount());
    }

    private boolean step2_ReserveStock(OrderSagaContext context) {
        transitionTo(context, SagaState.RESERVING_STOCK);
        log.info("[SAGA ORCHESTRATOR] [STEP 2] Nhạc trưởng phát lệnh giữ kho sang Product Service...");

        boolean reserved = productServiceClient.reserveStock(context.getReservationItems());
        if (reserved) {
            transitionTo(context, SagaState.STOCK_RESERVED);
            log.info("[SAGA ORCHESTRATOR] [STEP 2 OK] Product Service xác nhận giữ kho thành công.");
            return true;
        } else {
            log.warn("[SAGA ORCHESTRATOR] [STEP 2 FAIL] Product Service từ chối (Không đủ tồn kho).");
            context.setFailureMessage("Tồn kho không đủ để đáp ứng đơn hàng.");
            return false;
        }
    }

    private boolean step3_ProcessPayment(OrderSagaContext context) {
        transitionTo(context, SagaState.PROCESSING_PAYMENT);
        log.info("[SAGA ORCHESTRATOR] [STEP 3] Nhạc trưởng phát lệnh thanh toán sang Payment Service...");

        PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
                .orderId(context.getOrder().getId())
                .customerName(context.getOrder().getCustomerName())
                .amount(context.getTotalAmount())
                .paymentMethod(context.getPaymentMethod() != null ? context.getPaymentMethod() : "VNPAY")
                .build();

        PaymentResponseDto paymentResponse = paymentServiceClient.processPayment(paymentRequest);

        if (paymentResponse != null && "SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            transitionTo(context, SagaState.PAYMENT_SUCCESS);
            context.setPaymentTransactionId(paymentResponse.getPaymentId());
            log.info("[SAGA ORCHESTRATOR] [STEP 3 OK] Payment Service xác nhận thanh toán thành công! Mã GD: {}",
                    paymentResponse.getPaymentId());
            return true;
        } else {
            String reason = paymentResponse != null ? paymentResponse.getMessage() : "Payment Service không phản hồi";
            context.setFailureMessage(reason);
            log.warn("[SAGA ORCHESTRATOR] [STEP 3 FAIL] Thanh toán thất bại. Lý do: {}", reason);
            return false;
        }
    }

    private void step4_ConfirmOrder(OrderSagaContext context) {
        Order order = context.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("[SAGA ORCHESTRATOR] [STEP 4 OK] Nhạc trưởng chốt đơn hàng {} thành CONFIRMED.", order.getId());
    }

    // =========================================================================
    // CÁC HÀM GIAO DỊCH BÙ TRỪ (COMPENSATING ACTIONS)
    // =========================================================================

    private void compensate_ReleaseStock(OrderSagaContext context) {
        transitionTo(context, SagaState.COMPENSATING_STOCK);
        log.info("[SAGA ORCHESTRATOR] [COMPENSATING C2] Nhạc trưởng phát lệnh BÙ TRỪ: Yêu cầu Product Service hoàn lại kho...");

        boolean released = productServiceClient.releaseStock(context.getReservationItems());
        if (released) {
            transitionTo(context, SagaState.STOCK_RELEASED);
            log.info("[SAGA ORCHESTRATOR] [COMPENSATING C2 OK] Product Service đã hoàn trả tồn kho đầy đủ.");
        } else {
            log.error("[SAGA ORCHESTRATOR] [COMPENSATING C2 CRITICAL] Lỗi hoàn kho! Cần cơ chế Retry hoặc Alert!");
        }
    }

    private void cancelOrder(Order order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("[SAGA ORCHESTRATOR] [COMPENSATING C1 OK] Đơn hàng {} đã chuyển sang CANCELLED. Lý do: {}",
                order.getId(), reason);
    }

    // =========================================================================
    // QUẢN LÝ CHUYỂN TIẾP TRẠNG THÁI (STATE TRANSITION)
    // =========================================================================

    private void transitionTo(OrderSagaContext context, SagaState newState) {
        SagaState oldState = context.getCurrentState();
        context.setCurrentState(newState);
        log.info("  >>> [SAGA STATE MACHINE] CHUYỂN TRẠNG THÁI: {} ──► {}",
                oldState != null ? oldState : "NONE", newState);
    }
}
