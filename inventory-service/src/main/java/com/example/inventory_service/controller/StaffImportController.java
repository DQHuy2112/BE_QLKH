package com.example.inventory_service.controller;

import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;
import com.example.inventory_service.service.StaffImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller xử lý phiếu nhập từ nhân viên (STAFF)
 */
@RestController
@RequestMapping("/api/imports/staff")
@RequiredArgsConstructor
public class StaffImportController {

    private final StaffImportService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SupplierImportRequest request) {
        SupplierImportDto dto = service.create(request);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Tạo phiếu nhập từ nhân viên thành công",
                "data", dto));
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<SupplierImportDto> list = service.search(status, code, fromDate, toDate);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "data", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        SupplierImportDto dto = service.getById(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "data", dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SupplierImportRequest request) {
        SupplierImportDto dto = service.update(id, request);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Cập nhật phiếu nhập từ nhân viên thành công",
                "data", dto));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        SupplierImportDto dto = service.confirm(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Xác nhận nhập kho thành công",
                "data", dto));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        SupplierImportDto dto = service.cancel(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Hủy phiếu nhập từ nhân viên thành công",
                "data", dto));
    }
}
