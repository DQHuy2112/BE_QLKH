package com.example.inventory_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SupplierExportDto {
    private Long id;
    private String code;
    private Long storeId;
    private Long supplierId;
    private String supplierName;   // FE sẽ join từ product-service
    private String status;
    private Date exportsDate;
    private String note;
    private BigDecimal totalValue; // tổng tiền = sum(quantity * unit_price)
}
