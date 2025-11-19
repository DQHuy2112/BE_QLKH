package com.example.inventory_service.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class SupplierImportDto {

    private Long id;
    private String code;

    private Long storeId;
    private Long supplierId;
    private String supplierName;

    private String status;
    private Date importsDate;
    private String note;

    private BigDecimal totalValue;

    private List<String> attachmentImages; // trả ra FE: danh sách ảnh

    private List<ImportDetailDto> items;

}
