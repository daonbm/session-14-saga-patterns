package com.session06.order_service.client;

import com.session06.order_service.dto.ProductDto;
import com.session06.order_service.dto.StockOperationResponse;
import com.session06.order_service.dto.StockReservationItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.product-service.url:http://product-service}")
    private String productServiceUrl;

    @CircuitBreaker(name = "productService", fallbackMethod = "getAllProductsFallback")
    public List<ProductDto> getAllProducts() {
        String url = productServiceUrl + "/api/products";
        log.info("Sending GET request to Product Service via RestTemplate: {}", url);
        try {
            ResponseEntity<List<ProductDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch products from Product Service: {}", e.getMessage());
            throw new RuntimeException("Error communicating with Product Service: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    public Optional<ProductDto> getProductById(Long productId) {
        String url = productServiceUrl + "/api/products/" + productId;
        log.info("Sending GET request to Product Service for product ID {}: {}", productId, url);
        try {
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product with ID {} not found in Product Service", productId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch product {} from Product Service: {}", productId, e.getMessage());
            throw new RuntimeException("Error communicating with Product Service: " + e.getMessage(), e);
        }
    }

    /**
     * Saga Step 2: Reserve Stock in Product Service
     */
    public boolean reserveStock(List<StockReservationItem> items) {
        String url = productServiceUrl + "/api/products/reserve-stock";
        log.info("[SAGA ORCHESTRATOR -> PRODUCT] Gửi yêu cầu giữ kho: {}, Số mặt hàng: {}", url, items.size());
        try {
            HttpEntity<List<StockReservationItem>> entity = new HttpEntity<>(items);
            ResponseEntity<StockOperationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    StockOperationResponse.class
            );
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().isSuccess();
        } catch (Exception e) {
            log.warn("[SAGA ORCHESTRATOR -> PRODUCT] Lỗi giữ kho từ Product Service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Saga Compensating Action: Release Stock in Product Service
     */
    public boolean releaseStock(List<StockReservationItem> items) {
        String url = productServiceUrl + "/api/products/release-stock";
        log.info("[SAGA ORCHESTRATOR -> PRODUCT] [COMPENSATING ACTION] Gửi yêu cầu hoàn trả kho: {}, Số mặt hàng: {}", url, items.size());
        try {
            HttpEntity<List<StockReservationItem>> entity = new HttpEntity<>(items);
            ResponseEntity<StockOperationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    StockOperationResponse.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("[SAGA ORCHESTRATOR -> PRODUCT] LỖI NGHIÊM TRỌNG khi thực thi bù trừ hoàn kho: {}", e.getMessage());
            return false;
        }
    }

    // ==================== FALLBACK METHODS ====================

    /**
     * Fallback khi product-service không khả dụng hoặc Circuit Breaker OPEN.
     * Trả về empty list thay vì throw exception → tránh cascading failure.
     */
    public List<ProductDto> getAllProductsFallback(Throwable t) {
        log.warn("Circuit Breaker ACTIVATED for getAllProducts(). product-service không khả dụng. Reason: {}", t.getMessage());
        return Collections.emptyList();
    }

    /**
     * Fallback khi không lấy được product theo ID.
     * Trả về Optional.empty() → caller tự xử lý.
     */
    public Optional<ProductDto> getProductByIdFallback(Long productId, Throwable t) {
        log.warn("Circuit Breaker ACTIVATED for getProductById({}). product-service không khả dụng. Reason: {}", productId, t.getMessage());
        return Optional.empty();
    }
}
