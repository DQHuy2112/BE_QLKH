package com.example.product_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    private String code;
    private String name;
    private String shortDescription;
    private String image;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer minStock;
    private Integer maxStock;

    private String status;
    private Long categoryId;
    private Long supplierId;
    private Long unitId;
}
