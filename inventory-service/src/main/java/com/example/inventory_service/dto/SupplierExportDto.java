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
    private String storeName; // Tên kho xuất
    private String storeCode; // Mã kho

    private Long customerId; // ID khách hàng (bắt buộc)
    private String customerName; // Tên khách hàng
    private String customerPhone; // SĐT khách hàng
    private String customerAddress; // Địa chỉ khách hàng

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date exportsDate;

    private String note;
    private BigDecimal totalValue; // tổng tiền = sum(quantity * unit_price)
    private List<String> attachmentImages; // trả ra FE: danh sách ảnh
    private List<ExportDetailDto> items; // chi tiết sản phẩm

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

    private Long exportedBy; // userId của người xuất kho
    private String exportedByName; // Tên người xuất kho
    private String exportedByRole; // Vai trò người xuất kho
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date exportedAt; // Thời gian xuất kho

    // Warnings (ví dụ: các dòng item bị skip do dữ liệu không hợp lệ)
    private List<String> warnings;
}
