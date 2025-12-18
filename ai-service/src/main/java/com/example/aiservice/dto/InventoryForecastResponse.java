package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryForecastResponse {

    /**
     * Danh sách các SKU có nguy cơ thiếu hàng trong thời gian ngắn.
     */
    private List<ItemAtRisk> itemsAtRisk;

    /**
     * Danh sách các SKU đang tồn kho cao / dư hàng.
     */
    private List<OverstockItem> overstockItems;

    /**
     * Đoạn phân tích/tóm tắt tổng quan cho báo cáo tồn kho.
     */
    private String summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemAtRisk {
        private String code;
        private String name;
        private Integer quantity;
        /**
         * Số ngày ước tính còn đủ hàng với tốc độ bán hiện tại.
         */
        private Double daysRemaining;
        /**
         * Số lượng đề xuất nhập thêm để đạt mục tiêu tồn kho an toàn.
         */
        private Long recommendedPurchaseQty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverstockItem {
        private String code;
        private String name;
        private Integer quantity;
        /**
         * Số ngày ước tính có thể bán hết lượng tồn hiện tại.
         */
        private Double daysOfStock;
        /**
         * Gợi ý hành động (xả hàng, khuyến mãi, luân chuyển kho, ...).
         */
        private String recommendation;
    }
}

