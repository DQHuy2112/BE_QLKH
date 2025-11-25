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
    private String storeName; // Tên kho đích (kho nhận hàng)

    private Long supplierId;
    private String supplierName; // Tên nhà cung cấp/khách hàng (cho phiếu xuất NCC)
    private String supplierCode; // Mã nhà cung cấp
    private String supplierPhone; // Số điện thoại nhà cung cấp
    private String supplierAddress; // Địa chỉ nhà cung cấp

    private Long sourceStoreId; // ID kho nguồn (cho phiếu xuất nội bộ - nơi xuất hàng)
    private String sourceStoreName; // Tên kho nguồn (cho phiếu xuất nội bộ)
    private String sourceStoreCode; // Mã kho nguồn

    private Long targetStoreId; // ID kho đích (deprecated, dùng storeId)
    private String targetStoreName; // Tên kho đích (deprecated, dùng storeName)
    private String targetStoreCode; // Mã kho đích (deprecated)
    private String storeCode; // Mã kho

    private Long staffId; // ID nhân viên (cho phiếu xuất từ nhân viên)
    private String staffName; // Tên nhân viên (cho phiếu xuất từ nhân viên)

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date exportsDate;

    private String note;
    private BigDecimal totalValue; // tổng tiền = sum(quantity * unit_price)
    private List<String> attachmentImages; // trả ra FE: danh sách ảnh
    private List<ExportDetailDto> items; // chi tiết sản phẩm
}
