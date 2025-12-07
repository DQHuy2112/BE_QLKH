package com.example.inventory_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class InventoryCheckDto {
    private Long id;
    private String checkCode;

    private Long storeId;
    private String storeName;

    private String description;
    private String status; // PENDING, APPROVED, REJECTED

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date checkDate;

    private Long createdBy;
    private String createdByName;
    private String createdByRole;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date createdAt;

    private Long approvedBy;
    private String approvedByName;
    private String approvedByRole;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date approvedAt;

    private Long confirmedBy;
    private String confirmedByName;
    private String confirmedByRole;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date confirmedAt;

    private Long rejectedBy;
    private String rejectedByName;
    private String rejectedByRole;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date rejectedAt;

    private String note;
    private List<String> attachmentImages;

    private BigDecimal totalDifferenceValue; // Tổng giá trị chênh lệch

    private List<InventoryCheckDetailDto> items;
}
