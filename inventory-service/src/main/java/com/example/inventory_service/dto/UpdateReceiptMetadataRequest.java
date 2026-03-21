package com.example.inventory_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateReceiptMetadataRequest {
    private String receiptType;
    private String supplierName;
    private String customerName;
    private String receiptCode;
    private String receiptDate;
    private String note;
    private Double totalAmount;
    private List<ProductMetadata> products;

    @Data
    public static class ProductMetadata {
        private Long productId;
        private String name;
        private String code;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
        private String unit;
        private String warehouse;
    }
}

