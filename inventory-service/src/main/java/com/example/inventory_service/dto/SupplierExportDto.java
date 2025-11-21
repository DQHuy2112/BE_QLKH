package com.example.inventory_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class SupplierExportDto {
    private Long id;
    private String code;
    private Long storeId;
    private Long supplierId;
    private String supplierName; // FE sẽ join từ product-service
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date exportsDate;

    private String note;
    private BigDecimal totalValue; // tổng tiền = sum(quantity * unit_price)
    private List<String> attachmentImages; // trả ra FE: danh sách ảnh
    private List<ExportDetailDto> items; // chi tiết sản phẩm
}
