package com.example.inventory_service.controller;

import com.example.inventory_service.common.ApiResponse;
import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.service.ExportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService service;

    public ExportController(ExportService service) {
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

    @GetMapping("/search")
    public ApiResponse<Page<SupplierExportDto>> searchPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "date") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupplierExportDto> data = service.searchPaged(status, code, from, to, sortField, sortDir, pageable);
        return ApiResponse.ok(data);
    }

    // ================= DETAIL =====================
    @GetMapping("/{id:\\d+}")
    public ApiResponse<SupplierExportDto> getById(@PathVariable Long id) {
        SupplierExportDto dto = service.getById(id);
        return ApiResponse.ok(dto);
    }

    // ================= CREATE =====================
    @PostMapping
    public ApiResponse<SupplierExportDto> create(
            @RequestBody SupplierExportRequest request) {
        SupplierExportDto dto = service.create(request);
        return ApiResponse.ok("Created", dto);
    }

    // ================= UPDATE =====================
    @PutMapping("/{id}")
    public ApiResponse<SupplierExportDto> update(
            @PathVariable Long id,
            @RequestBody SupplierExportRequest request) {
        SupplierExportDto dto = service.update(id, request);
        return ApiResponse.ok("Updated", dto);
    }

    // ================= APPROVE (PENDING → APPROVED) =====================
    @PostMapping("/{id}/approve")
    public ApiResponse<SupplierExportDto> approve(@PathVariable Long id) {
        SupplierExportDto dto = service.approve(id);
        return ApiResponse.ok("Đã duyệt phiếu xuất (chờ xuất kho)", dto);
    }

    // ================= CONFIRM (APPROVED → EXPORTED) =====================
    @PostMapping("/{id}/confirm")
    public ApiResponse<SupplierExportDto> confirm(@PathVariable Long id) {
        SupplierExportDto dto = service.confirm(id);
        return ApiResponse.ok("Đã xác nhận xuất kho", dto);
    }

    // ================= CANCEL (PENDING → CANCELLED) =====================
    @PostMapping("/{id}/cancel")
    public ApiResponse<SupplierExportDto> cancel(@PathVariable Long id) {
        SupplierExportDto dto = service.cancel(id);
        return ApiResponse.ok("Đã hủy phiếu xuất", dto);
    }

    // ================= REJECT (PENDING → REJECTED) =====================
    @PostMapping("/{id}/reject")
    public ApiResponse<SupplierExportDto> reject(@PathVariable Long id) {
        SupplierExportDto dto = service.reject(id);
        return ApiResponse.ok("Đã từ chối phiếu xuất", dto);
    }

    // ================= GET ALL =====================
    @GetMapping("/all")
    public ApiResponse<List<SupplierExportDto>> getAll() {
        return ApiResponse.ok(service.getAll());
    }

    // ================= GET BY STORE =====================
    @GetMapping("/by-store/{storeId}")
    public ApiResponse<List<SupplierExportDto>> getByStore(@PathVariable Long storeId) {
        return ApiResponse.ok(service.getByStore(storeId));
    }

    // ================= GET BY ORDER =====================
    @GetMapping("/by-order/{orderId}")
    public ApiResponse<List<SupplierExportDto>> getByOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(service.getByOrder(orderId));
    }
}
