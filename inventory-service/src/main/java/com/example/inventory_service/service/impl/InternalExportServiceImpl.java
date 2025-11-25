package com.example.inventory_service.service.impl;

import com.example.inventory_service.client.ProductServiceClient;
import com.example.inventory_service.dto.ExportDetailDto;
import com.example.inventory_service.dto.ExportDetailRequest;
import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.entity.ShopExport;
import com.example.inventory_service.entity.ShopExportDetail;
import com.example.inventory_service.exception.NotFoundException;
import com.example.inventory_service.repository.ShopExportDetailRepository;
import com.example.inventory_service.repository.ShopExportRepository;
import com.example.inventory_service.service.InternalExportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý phiếu xuất nội bộ (INTERNAL)
 * Xuất hàng từ kho này sang kho khác
 */
@Service
public class InternalExportServiceImpl implements InternalExportService {

    private final ShopExportRepository exportRepo;
    private final ShopExportDetailRepository detailRepo;
    private final ProductServiceClient productClient;
    private final com.example.inventory_service.repository.ShopStoreRepository storeRepo;

    public InternalExportServiceImpl(
            ShopExportRepository exportRepo,
            ShopExportDetailRepository detailRepo,
            ProductServiceClient productClient,
            com.example.inventory_service.repository.ShopStoreRepository storeRepo) {
        this.exportRepo = exportRepo;
        this.detailRepo = detailRepo;
        this.productClient = productClient;
        this.storeRepo = storeRepo;
    }

    @Override
    @Transactional
    public SupplierExportDto create(SupplierExportRequest request) {
        java.util.Date now = new java.util.Date();

        ShopExport ex = new ShopExport();

        if (request.getCode() != null && !request.getCode().isBlank()) {
            ex.setCode(request.getCode());
        } else {
            ex.setCode(generateCode());
        }

        ex.setExportType("INTERNAL"); // ⭐ Phiếu xuất nội bộ
        ex.setStoreId(request.getStoreId()); // Kho đích (kho nhận hàng)
        // Với phiếu nội bộ, supplierId lưu sourceStoreId (kho nguồn - nơi xuất hàng)
        // Hỗ trợ cả sourceStoreId (mới) và targetStoreId (cũ) để backward compatible
        Long sourceStore = request.getSourceStoreId();
        if (sourceStore == null && request.getTargetStoreId() != null) {
            // Nếu frontend gửi targetStoreId (cũ), swap logic: targetStoreId là kho nguồn
            sourceStore = request.getTargetStoreId();
        }
        if (sourceStore == null) {
            sourceStore = request.getSupplierId();
        }
        ex.setSupplierId(sourceStore);
        ex.setNote(limitNote(request.getNote()));
        ex.setDescription(request.getDescription());
        ex.setStatus("PENDING");
        ex.setExportsDate(now);
        ex.setCreatedAt(now);
        ex.setUpdatedAt(now);

        // Lưu ảnh
        if (request.getAttachmentImages() != null && !request.getAttachmentImages().isEmpty()) {
            String joined = request.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            ex.setAttachmentImage(joined);
        } else {
            ex.setAttachmentImage(null);
        }

        ex = exportRepo.save(ex);

        // Lưu chi tiết
        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (ExportDetailRequest item : request.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0)
                    continue;
                if (item.getUnitPrice() == null)
                    continue;

                ShopExportDetail d = new ShopExportDetail();
                d.setExportId(ex.getId());
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent()); // ✅ THÊM DISCOUNT

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(ex, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierExportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        // Chỉ lấy phiếu INTERNAL
        List<ShopExport> list = exportRepo.searchInternalExports(
                status,
                code,
                fromDate,
                toDate);

        List<SupplierExportDto> result = new ArrayList<>();
        for (ShopExport ex : list) {
            result.add(toDtoWithCalcTotal(ex));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierExportDto getById(Long id) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Internal export not found: " + id));

        // Kiểm tra có phải INTERNAL không
        if (!"INTERNAL".equals(ex.getExportType())) {
            throw new IllegalStateException("This is not an internal export");
        }

        return toDtoWithCalcTotal(ex);
    }

    @Override
    @Transactional
    public SupplierExportDto update(Long id, SupplierExportRequest request) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Internal export not found: " + id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            ex.setCode(request.getCode());
        }
        ex.setStoreId(request.getStoreId());

        // Hỗ trợ cả sourceStoreId (mới) và targetStoreId (cũ) để backward compatible
        Long sourceStore = request.getSourceStoreId();
        if (sourceStore == null && request.getTargetStoreId() != null) {
            sourceStore = request.getTargetStoreId();
        }
        if (sourceStore == null) {
            sourceStore = request.getSupplierId();
        }
        ex.setSupplierId(sourceStore);
        ex.setNote(limitNote(request.getNote()));
        ex.setDescription(request.getDescription());
        ex.setUpdatedAt(new java.util.Date());

        // Cập nhật ảnh
        if (request.getAttachmentImages() != null && !request.getAttachmentImages().isEmpty()) {
            String joined = request.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            ex.setAttachmentImage(joined);
        } else {
            ex.setAttachmentImage(null);
        }

        ex = exportRepo.save(ex);

        // Xóa chi tiết cũ và tạo mới
        detailRepo.deleteByExportId(id);

        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (ExportDetailRequest item : request.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0)
                    continue;
                if (item.getUnitPrice() == null)
                    continue;

                ShopExportDetail d = new ShopExportDetail();
                d.setExportId(ex.getId());
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent()); // ✅ THÊM DISCOUNT

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(ex, total);
    }

    @Override
    @Transactional
    public SupplierExportDto confirm(Long id) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Internal export not found: " + id));

        if (!"PENDING".equals(ex.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xác nhận phiếu đang ở trạng thái PENDING");
        }

        ex.setStatus("EXPORTED");
        ex.setUpdatedAt(new java.util.Date());
        ex = exportRepo.save(ex);

        // Cập nhật tồn kho: Giảm số lượng
        List<ShopExportDetail> details = detailRepo.findByExportId(id);
        for (ShopExportDetail d : details) {
            if (d.getQuantity() != null && d.getQuantity() > 0) {
                productClient.decreaseQuantity(d.getProductId(), d.getQuantity());
            }
        }

        return toDtoWithCalcTotal(ex);
    }

    @Override
    @Transactional
    public SupplierExportDto cancel(Long id) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Internal export not found: " + id));

        if (!"PENDING".equals(ex.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy phiếu đang ở trạng thái PENDING");
        }

        ex.setStatus("CANCELLED");
        ex.setUpdatedAt(new java.util.Date());
        ex = exportRepo.save(ex);

        return toDtoWithCalcTotal(ex);
    }

    // ========= HELPER METHODS ========= //

    private String generateCode() {
        return "PXNB" + System.currentTimeMillis(); // Phiếu Xuất Nội Bộ
    }

    private String limitNote(String note) {
        if (note == null)
            return null;
        int max = 255;
        return note.length() > max ? note.substring(0, max) : note;
    }

    private String normalizeImagePath(String raw) {
        if (raw == null || raw.isBlank())
            return null;

        int idx = raw.indexOf("/uploads/");
        if (idx >= 0) {
            return raw.substring(idx);
        }

        if (!raw.startsWith("/")) {
            return "/" + raw;
        }

        return raw;
    }

    private SupplierExportDto toDtoWithCalcTotal(ShopExport ex) {
        List<ShopExportDetail> details = detailRepo.findByExportId(ex.getId());
        BigDecimal total = BigDecimal.ZERO;
        List<ExportDetailDto> itemDtos = new ArrayList<>();

        if (details != null) {
            for (ShopExportDetail d : details) {
                if (d.getUnitPrice() == null || d.getQuantity() == null)
                    continue;

                BigDecimal line = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
                total = total.add(line);

                ExportDetailDto itemDto = new ExportDetailDto();
                itemDto.setId(d.getId());
                itemDto.setProductId(d.getProductId());
                itemDto.setQuantity(d.getQuantity());
                itemDto.setUnitPrice(d.getUnitPrice());
                itemDto.setDiscountPercent(d.getDiscountPercent()); // ✅ Trả về % chiết khấu
                itemDto.setProductCode(null);
                itemDto.setProductName(null);
                itemDto.setUnit(null);

                itemDtos.add(itemDto);
            }
        }

        SupplierExportDto dto = toDto(ex, total);
        dto.setItems(itemDtos);
        return dto;
    }

    private SupplierExportDto toDto(ShopExport exp, BigDecimal total) {
        SupplierExportDto dto = new SupplierExportDto();
        dto.setId(exp.getId());
        dto.setCode(exp.getCode());
        dto.setStoreId(exp.getStoreId());
        dto.setSupplierId(exp.getSupplierId()); // Đây là sourceStoreId cho phiếu nội bộ
        dto.setStatus(exp.getStatus());
        dto.setExportsDate(exp.getExportsDate());
        dto.setNote(exp.getNote());
        dto.setTotalValue(total);

        // Lấy thông tin kho đích (kho nhận hàng)
        if (exp.getStoreId() != null) {
            storeRepo.findById(exp.getStoreId()).ifPresent(store -> {
                dto.setStoreName(store.getName());
            });
        }

        // Lấy thông tin kho nguồn (supplierId trong phiếu nội bộ là sourceStoreId)
        if (exp.getSupplierId() != null) {
            dto.setSourceStoreId(exp.getSupplierId());
            storeRepo.findById(exp.getSupplierId()).ifPresent(store -> {
                dto.setSourceStoreName(store.getName());
                // ⭐ Gán thông tin kho nguồn vào supplier fields để frontend hiển thị giống
                // phiếu NCC
                dto.setSupplierName(store.getName());
                dto.setSupplierCode(store.getCode());
            });
        } else {
            dto.setSupplierName(null);
            dto.setSupplierCode(null);
        }

        // Map ảnh
        List<String> images = new ArrayList<>();
        String raw = exp.getAttachmentImage();
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
