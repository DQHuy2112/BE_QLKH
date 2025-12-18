package com.example.inventory_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class SupplierImportDto {

    private Long id;
    private String code;

    private Long storeId;
    private String storeName; // Tên kho đích (kho nhận hàng)
    private String storeCode; // Mã kho đích (kho nhận hàng)

    private Long supplierId;
    private String supplierName; // Tên NCC (cho phiếu nhập từ NCC)
    private String supplierCode; // Mã nhà cung cấp
    private String supplierPhone; // Số điện thoại nhà cung cấp
    private String supplierAddress; // Địa chỉ nhà cung cấp

    private Long sourceStoreId; // ID kho nguồn (cho phiếu nhập nội bộ)
    private String sourceStoreName; // Tên kho nguồn (cho phiếu nhập nội bộ)

    private Long staffId; // ID nhân viên (cho phiếu nhập từ nhân viên)
    private String staffName; // Tên nhân viên (cho phiếu nhập từ nhân viên)

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date importsDate;

    private String note;

    private BigDecimal totalValue;

    private List<String> attachmentImages; // trả ra FE: danh sách ảnh

    private List<ImportDetailDto> items;

    // Audit fields
    private Long createdBy; // userId của người tạo
    private String createdByName; // Tên người tạo
    private String createdByRole; // Vai trò người tạo
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdAt; // Thời gian tạo

    private Long approvedBy; // userId của người duyệt
    private String approvedByName; // Tên người duyệt
    private String approvedByRole; // Vai trò người duyệt
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date approvedAt; // Thời gian duyệt

    private Long rejectedBy; // userId của người từ chối
    private String rejectedByName; // Tên người từ chối
    private String rejectedByRole; // Vai trò người từ chối
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date rejectedAt; // Thời gian từ chối

    private Long importedBy; // userId của người nhập kho
    private String importedByName; // Tên người nhập kho
    private String importedByRole; // Vai trò người nhập kho
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date importedAt; // Thời gian nhập kho

    // Warnings (ví dụ: các dòng item bị skip do dữ liệu không hợp lệ)
    private List<String> warnings;

}
