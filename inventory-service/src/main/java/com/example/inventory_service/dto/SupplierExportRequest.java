package com.example.inventory_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class SupplierExportRequest {
    private String code; // optional, nếu null BE tự sinh
    private Long storeId; // Kho đích (kho nhận hàng)
    private Long supplierId; // Nhà cung cấp (cho phiếu xuất NCC)
    private Long sourceStoreId; // Kho nguồn (cho phiếu xuất nội bộ - nơi xuất hàng)
    private Long targetStoreId; // Kho đích (cho phiếu xuất nội bộ - deprecated, dùng storeId)
    private Long staffId; // ID nhân viên (cho phiếu xuất từ nhân viên)
    private String note;
    private String description;
    private List<String> attachmentImages; // đường dẫn ảnh FE gửi (/uploads/... hoặc full URL)
    private List<ExportDetailRequest> items;
}
