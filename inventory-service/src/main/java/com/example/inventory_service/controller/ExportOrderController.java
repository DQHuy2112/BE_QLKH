package com.example.inventory_service.controller;

import com.example.inventory_service.common.ApiResponse;
import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.service.ExportOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders/exports")
public class ExportOrderController {

    private final ExportOrderService service;

    public ExportOrderController(ExportOrderService service) {
        this.service = service;
    }

    // ================= SEARCH =====================
    @GetMapping
    public ApiResponse<List<SupplierExportDto>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SupplierExportDto> data = service.search(status, code, from, to);
        return ApiResponse.ok(data);
    }

    // ================= DETAIL =====================
    @GetMapping("/{id}")
    public ApiResponse<SupplierExportDto> getById(@PathVariable Long id) {
        SupplierExportDto dto = service.getById(id);
        return ApiResponse.ok(dto);
    }

    // ================= CREATE =====================
    @PostMapping
    public ApiResponse<SupplierExportDto> create(
            @RequestBody SupplierExportRequest request) {
        SupplierExportDto dto = service.create(request);
        return ApiResponse.ok(dto);
    }

    // ================= UPDATE =====================
    @PutMapping("/{id}")
    public ApiResponse<SupplierExportDto> update(
            @PathVariable Long id,
            @RequestBody SupplierExportRequest request) {
        SupplierExportDto dto = service.update(id, request);
        return ApiResponse.ok(dto);
    }

    // ================= APPROVE (PENDING → APPROVED) =====================
    @PostMapping("/{id}/approve")
    public ApiResponse<SupplierExportDto> approve(@PathVariable Long id) {
        SupplierExportDto dto = service.approve(id);
        return ApiResponse.ok("Đã duyệt lệnh xuất kho", dto);
    }

    // ================= CANCEL (PENDING → CANCELLED) =====================
    @PostMapping("/{id}/cancel")
    public ApiResponse<SupplierExportDto> cancel(@PathVariable Long id) {
        SupplierExportDto dto = service.cancel(id);
        return ApiResponse.ok("Đã hủy lệnh xuất kho", dto);
    }
}
