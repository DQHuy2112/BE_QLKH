package com.example.inventory_service.controller;

import com.example.inventory_service.common.ApiResponse;
import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.service.SupplierExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exports/suppliers")
public class SupplierExportController {

    private final SupplierExportService service;

    public SupplierExportController(SupplierExportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SupplierExportDto>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(service.search(status, code, fromDate, toDate));
    }

    @PostMapping
    public ApiResponse<SupplierExportDto> create(
            @RequestBody SupplierExportRequest request) {
        return ApiResponse.ok("Created", service.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierExportDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierExportDto> update(
            @PathVariable Long id,
            @RequestBody SupplierExportRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
