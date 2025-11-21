package com.example.inventory_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client để gọi API sang Product-service
 */
@Component
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8081}")
    private String productServiceUrl;

    public ProductServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        System.out.println("🔧 ProductServiceClient initialized with URL: " + productServiceUrl);
    }

    /**
     * Cập nhật số lượng tồn kho của sản phẩm
     * 
     * @param productId ID sản phẩm
     * @param quantity  Số lượng mới
     */
    public void updateProductQuantity(Long productId, int quantity) {
        String url = productServiceUrl + "/api/products/" + productId + "/quantity";

        try {
            restTemplate.put(url, new UpdateQuantityRequest(quantity));
        } catch (Exception e) {
            // Log lỗi nhưng không throw để không ảnh hưởng luồng chính
            System.err.println("Failed to update product quantity: " + e.getMessage());
        }
    }

    /**
     * Tăng số lượng tồn kho
     */
    public void increaseQuantity(Long productId, int amount) {
        String url = productServiceUrl + "/api/products/" + productId + "/quantity/increase";

        try {
            System.out.println("🔵 Calling Product-service: " + url + " with amount: " + amount);
            restTemplate.postForObject(url, new QuantityChangeRequest(amount), Void.class);
            System.out.println("✅ Successfully increased quantity for product: " + productId);
        } catch (Exception e) {
            System.err.println("❌ Failed to increase product quantity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Giảm số lượng tồn kho
     */
    public void decreaseQuantity(Long productId, int amount) {
        String url = productServiceUrl + "/api/products/" + productId + "/quantity/decrease";

        try {
            System.out.println("🔵 Calling Product-service: " + url + " with amount: " + amount);
            restTemplate.postForObject(url, new QuantityChangeRequest(amount), Void.class);
            System.out.println("✅ Successfully decreased quantity for product: " + productId);
        } catch (Exception e) {
            System.err.println("❌ Failed to decrease product quantity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // DTO classes
    private static class UpdateQuantityRequest {
        public int quantity;

        public UpdateQuantityRequest(int quantity) {
            this.quantity = quantity;
        }
    }

    private static class QuantityChangeRequest {
        public int amount;

        public QuantityChangeRequest(int amount) {
            this.amount = amount;
        }
    }
}
