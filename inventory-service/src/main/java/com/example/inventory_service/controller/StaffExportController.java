package com.example.inventory_service.controller;

import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.service.StaffExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller xử lý phiếu xuất cho nhân viên (STAFF)
 */
@RestController
@RequestMapping("/api/exports/staff")
@RequiredArgsConstructor
public class StaffExportController {

    private final StaffExportService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SupplierExportRequest request) {
        SupplierExportDto dto = service.create(request);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Tạo phiếu xuất cho nhân viên thành công",
                "data", dto));
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<SupplierExportDto> list = service.search(status, code, fromDate, toDate);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "data", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        SupplierExportDto dto = service.getById(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "data", dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SupplierExportRequest request) {
        SupplierExportDto dto = service.update(id, request);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Cập nhật phiếu xuất cho nhân viên thành công",
                "data", dto));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        SupplierExportDto dto = service.confirm(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Xác nhận xuất kho thành công",
                "data", dto));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        SupplierExportDto dto = service.cancel(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Hủy phiếu xuất cho nhân viên thành công",
                "data", dto));
    }
}
