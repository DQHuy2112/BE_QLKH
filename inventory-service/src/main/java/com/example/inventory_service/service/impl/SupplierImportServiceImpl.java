package com.example.inventory_service.service.impl;

import com.example.inventory_service.dto.ImportDetailDto;
import com.example.inventory_service.dto.ImportDetailRequest;
import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;
import com.example.inventory_service.entity.ShopImport;
import com.example.inventory_service.entity.ShopImportDetail;
import com.example.inventory_service.exception.NotFoundException;
import com.example.inventory_service.repository.ShopImportDetailRepository;
import com.example.inventory_service.repository.ShopImportRepository;
import com.example.inventory_service.service.SupplierImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierImportServiceImpl implements SupplierImportService {

    private final ShopImportRepository importRepo;
    private final ShopImportDetailRepository detailRepo;

    public SupplierImportServiceImpl(
            ShopImportRepository importRepo,
            ShopImportDetailRepository detailRepo) {
        this.importRepo = importRepo;
        this.detailRepo = detailRepo;
    }

    // =========================================================
    // CREATE PHIẾU NHẬP NCC
    // =========================================================
    @Override
    @Transactional
    public SupplierImportDto create(SupplierImportRequest request) {
        java.util.Date now = new java.util.Date();

        // --------- Tạo phiếu nhập (ShopImport) ---------
        ShopImport im = new ShopImport();

        // nếu client gửi code thì dùng, không thì tự sinh
        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        } else {
            im.setCode(generateCode());
        }

        im.setImportType("SUPPLIER");
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getSupplierId());
        im.setNote(limitNote(request.getNote()));
        im.setDescription(request.getDescription());
        im.setStatus("PENDING");
        im.setImportsDate(now);
        im.setCreatedAt(now);
        im.setUpdatedAt(now);
        // TODO: nếu có JWT thì set userId từ token

        // ===== LƯU NHIỀU ẢNH VÀO 1 CỘT attachment_image =====
        if (request.getAttachmentImages() != null && !request.getAttachmentImages().isEmpty()) {
            String joined = request.getAttachmentImages().stream()
                    .map(this::normalizeImagePath) // chuẩn hoá /uploads/...
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";")); // path1;path2;path3

            im.setAttachmentImage(joined);
        } else {
            im.setAttachmentImage(null);
        }

        // lưu phiếu nhập
        im = importRepo.save(im);

        // --------- Lưu chi tiết phiếu nhập ---------
        BigDecimal total = BigDecimal.ZERO;
        List<ShopImportDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (ImportDetailRequest item : request.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0)
                    continue;
                if (item.getUnitPrice() == null)
                    continue;

                ShopImportDetail d = new ShopImportDetail();
                d.setImportId(im.getId());
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        // TODO: cập nhật tồn kho nếu bạn có bảng stocks

        return toDto(im, total);
    }

    // =========================================================
    // SEARCH PHIẾU NHẬP NCC
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public List<SupplierImportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        List<ShopImport> list = importRepo.searchSupplierImports(
                status,
                code,
                fromDate,
                toDate);

        List<SupplierImportDto> result = new ArrayList<>();
        for (ShopImport im : list) {
            result.add(toDtoWithCalcTotal(im));
        }
        return result;
    }

    // =========================================================
    // LẤY CHI TIẾT 1 PHIẾU NHẬP NCC
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public SupplierImportDto getById(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        return toDtoWithCalcTotal(im);
    }

    // =========================================================
    // UPDATE PHIẾU NHẬP NCC
    // =========================================================
    @Override
    @Transactional
    public SupplierImportDto update(Long id, SupplierImportRequest request) {
        // Tìm phiếu nhập cũ
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        // Cập nhật thông tin phiếu nhập
        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        }
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getSupplierId());
        im.setNote(limitNote(request.getNote()));
        im.setDescription(request.getDescription());
        im.setUpdatedAt(new java.util.Date());

        // Cập nhật ảnh
        if (request.getAttachmentImages() != null && !request.getAttachmentImages().isEmpty()) {
            String joined = request.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            im.setAttachmentImage(joined);
        } else {
            im.setAttachmentImage(null);
        }

        im = importRepo.save(im);

        // Xóa chi tiết cũ và tạo mới
        detailRepo.deleteByImportId(id);

        BigDecimal total = BigDecimal.ZERO;
        List<ShopImportDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (ImportDetailRequest item : request.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0)
                    continue;
                if (item.getUnitPrice() == null)
                    continue;

                ShopImportDetail d = new ShopImportDetail();
                d.setImportId(im.getId());
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(im, total);
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    // sinh mã phiếu nhập NCC, tạm dùng timestamp
    private String generateCode() {
        return "PNNCC" + System.currentTimeMillis();
    }

    // tránh lỗi "Data too long for column 'note'"
    private String limitNote(String note) {
        if (note == null)
            return null;
        int max = 255;
        return note.length() > max ? note.substring(0, max) : note;
    }

    // luôn lưu dạng /uploads/... để khớp WebConfig + FE
    private String normalizeImagePath(String raw) {
        if (raw == null || raw.isBlank())
            return null;

        // nếu FE gửi full URL http://.../uploads/...
        int idx = raw.indexOf("/uploads/");
        if (idx >= 0) {
            return raw.substring(idx);
        }

        // nếu FE gửi tương đối mà thiếu dấu /
        if (!raw.startsWith("/")) {
            return "/" + raw;
        }

        return raw;
    }

    // Tính lại tổng tiền từ bảng chi tiết
    private SupplierImportDto toDtoWithCalcTotal(ShopImport im) {
        List<ShopImportDetail> details = detailRepo.findByImportId(im.getId());
        BigDecimal total = BigDecimal.ZERO;
        List<ImportDetailDto> itemDtos = new ArrayList<>();

        if (details != null) {
            for (ShopImportDetail d : details) {
                if (d.getUnitPrice() == null || d.getQuantity() == null)
                    continue;

                BigDecimal line = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
                total = total.add(line);

                // Map sang DTO
                ImportDetailDto itemDto = new ImportDetailDto();
                itemDto.setId(d.getId());
                itemDto.setProductId(d.getProductId());
                itemDto.setQuantity(d.getQuantity());
                itemDto.setUnitPrice(d.getUnitPrice());

                // FE sẽ tự join thông tin sản phẩm từ product-service
                itemDto.setProductCode(null);
                itemDto.setProductName(null);
                itemDto.setUnit(null);

                itemDtos.add(itemDto);
            }
        }

        SupplierImportDto dto = toDto(im, total);
        dto.setItems(itemDtos); // ⭐ Thêm dòng này
        return dto;
    }

    // map entity -> DTO
    private SupplierImportDto toDto(ShopImport imp, BigDecimal total) {
        SupplierImportDto dto = new SupplierImportDto();
        dto.setId(imp.getId());
        dto.setCode(imp.getCode());
        dto.setStoreId(imp.getStoreId());
        dto.setSupplierId(imp.getSupplierId());
        dto.setStatus(imp.getStatus());
        dto.setImportsDate(imp.getImportsDate());
        dto.setNote(imp.getNote());
        dto.setTotalValue(total);

        dto.setSupplierName(null);

        // ==== map nhiều ảnh từ 1 cột attachment_image ====
        List<String> images = new ArrayList<>();
        String raw = imp.getAttachmentImage();
        if (raw != null && !raw.isBlank()) {
            images = Arrays.stream(raw.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        dto.setAttachmentImages(images);

        return dto;
    }

}
