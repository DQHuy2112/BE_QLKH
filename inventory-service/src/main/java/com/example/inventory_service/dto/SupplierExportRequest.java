package com.example.inventory_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class SupplierExportRequest {
    private String code; // optional, nếu null BE tự sinh
    private Long storeId;
    private Long supplierId;
    private String note;
    private String description;
    private List<String> attachmentImages; // đường dẫn ảnh FE gửi (/uploads/... hoặc full URL)
    private List<ExportDetailRequest> items;
}
