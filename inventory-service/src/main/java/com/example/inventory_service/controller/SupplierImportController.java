package com.example.inventory_service.controller;

import com.example.inventory_service.common.ApiResponse;
import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;
import com.example.inventory_service.service.SupplierImportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/imports/suppliers")
public class SupplierImportController {

    private final SupplierImportService service;

    public SupplierImportController(SupplierImportService service) {
        this.service = service;
    }

    // ================= SEARCH =====================
    @GetMapping
    public ApiResponse<List<SupplierImportDto>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SupplierImportDto> data = service.search(status, code, from, to);
        return ApiResponse.ok(data);
    }

    // ================= DETAIL =====================
    @GetMapping("/{id}")
    public ApiResponse<SupplierImportDto> getById(@PathVariable Long id) {
        SupplierImportDto dto = service.getById(id);
        return ApiResponse.ok(dto);
    }

    // ================= CREATE =====================
    @PostMapping
    public ApiResponse<SupplierImportDto> create(
            @RequestBody SupplierImportRequest request) {
        SupplierImportDto dto = service.create(request);
        return ApiResponse.ok(dto);
    }

    // ================= UPDATE =====================
    @PutMapping("/{id}")
    public ApiResponse<SupplierImportDto> update(
            @PathVariable Long id,
            @RequestBody SupplierImportRequest request) {
        SupplierImportDto dto = service.update(id, request);
        return ApiResponse.ok(dto);
    }

}
