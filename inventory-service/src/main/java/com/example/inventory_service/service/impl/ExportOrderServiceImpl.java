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
import com.example.inventory_service.service.ExportOrderService;
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
 * Service xử lý lệnh xuất kho (ORDER)
 * Khác với phiếu xuất: Lệnh chỉ là đặt hàng, chưa xuất kho thật
 * export_type = "ORDER"
 */
@Service
public class ExportOrderServiceImpl implements ExportOrderService {

    private final ShopExportRepository exportRepo;
    private final ShopExportDetailRepository detailRepo;
    private final ProductServiceClient productServiceClient;
    private final com.example.inventory_service.repository.ShopStoreRepository storeRepo;

    public ExportOrderServiceImpl(
            ShopExportRepository exportRepo,
            ShopExportDetailRepository detailRepo,
            ProductServiceClient productServiceClient,
            com.example.inventory_service.repository.ShopStoreRepository storeRepo) {
        this.exportRepo = exportRepo;
        this.detailRepo = detailRepo;
        this.productServiceClient = productServiceClient;
        this.storeRepo = storeRepo;
    }

    @Override
    @Transactional
    public SupplierExportDto create(SupplierExportRequest request) {
        // ⭐ Validation: Bắt buộc phải có supplierId cho Export ORDER
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("supplierId is required for Export ORDER");
        }

        java.util.Date now = new java.util.Date();

        ShopExport ex = new ShopExport();

        if (request.getCode() != null && !request.getCode().isBlank()) {
            ex.setCode(request.getCode());
        } else {
            ex.setCode(generateCode());
        }

        ex.setExportType("ORDER"); // ⭐ Lệnh xuất kho
        ex.setStoreId(request.getStoreId());
        ex.setSupplierId(request.getSupplierId());
        ex.setNote(limitNote(request.getNote()));
        ex.setDescription(request.getDescription());
        ex.setStatus("PENDING"); // Chờ duyệt
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

        // Chỉ lấy lệnh ORDER
        List<ShopExport> list = exportRepo.searchExportOrders(
                status,
                code,
                fromDate,
                toDate);

        // ⭐ CACHE suppliers và stores để tránh N+1 query
        java.util.Map<Long, ProductServiceClient.SupplierDto> supplierCache = new java.util.HashMap<>();
        java.util.Map<Long, String> storeCache = new java.util.HashMap<>();

        List<SupplierExportDto> result = new ArrayList<>();
        for (ShopExport ex : list) {
            result.add(toDtoWithCalcTotal(ex, supplierCache, storeCache));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierExportDto getById(Long id) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export order not found: " + id));

        // Kiểm tra có phải ORDER không
        if (!"ORDER".equals(ex.getExportType())) {
            throw new IllegalStateException("This is not an export order");
        }

        return toDtoWithCalcTotal(ex);
    }

    @Override
    @Transactional
    public SupplierExportDto update(Long id, SupplierExportRequest request) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export order not found: " + id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            ex.setCode(request.getCode());
        }
        ex.setStoreId(request.getStoreId());
        ex.setSupplierId(request.getSupplierId());
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
    public SupplierExportDto approve(Long id) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export order not found: " + id));

        if (!"PENDING".equals(ex.getStatus())) {
            throw new IllegalStateException("Chỉ có thể duyệt lệnh đang ở trạng thái PENDING");
        }

        // Duyệt lệnh → APPROVED (chưa xuất kho, không cập nhật tồn)
        ex.setStatus("APPROVED");
        ex.setUpdatedAt(new java.util.Date());
        ex = exportRepo.save(ex);

        return toDtoWithCalcTotal(ex);
    }

    @Override
    @Transactional
    public SupplierExportDto cancel(Long id) {
        ShopExport ex = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export order not found: " + id));

        if (!"PENDING".equals(ex.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy lệnh đang ở trạng thái PENDING");
        }

        ex.setStatus("CANCELLED");
        ex.setUpdatedAt(new java.util.Date());
        ex = exportRepo.save(ex);

        return toDtoWithCalcTotal(ex);
    }

    // ========= HELPER METHODS ========= //

    private String generateCode() {
        return "LXK" + System.currentTimeMillis(); // Lệnh Xuất Kho
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
        return toDtoWithCalcTotal(ex, new java.util.HashMap<>(), new java.util.HashMap<>());
    }

    private SupplierExportDto toDtoWithCalcTotal(
            ShopExport ex,
            java.util.Map<Long, ProductServiceClient.SupplierDto> supplierCache,
            java.util.Map<Long, String> storeCache) {
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

        SupplierExportDto dto = toDto(ex, total, supplierCache, storeCache);
        dto.setItems(itemDtos);
        return dto;
    }

    private SupplierExportDto toDto(ShopExport exp, BigDecimal total) {
        return toDto(exp, total, new java.util.HashMap<>(), new java.util.HashMap<>());
    }

    private SupplierExportDto toDto(
            ShopExport exp,
            BigDecimal total,
            java.util.Map<Long, ProductServiceClient.SupplierDto> supplierCache,
            java.util.Map<Long, String> storeCache) {
        SupplierExportDto dto = new SupplierExportDto();
        dto.setId(exp.getId());
        dto.setCode(exp.getCode());
        dto.setStoreId(exp.getStoreId());
        dto.setSupplierId(exp.getSupplierId());
        dto.setStatus(exp.getStatus());
        dto.setExportsDate(exp.getExportsDate());
        dto.setNote(exp.getNote());
        dto.setTotalValue(total);

        // ⭐ Load supplier name với CACHE
        if (exp.getSupplierId() != null) {
            try {
                ProductServiceClient.SupplierDto supplier = supplierCache.get(exp.getSupplierId());
                if (supplier == null) {
                    System.out.println("🔵 Calling Product-service (NOT IN CACHE): supplier_id=" + exp.getSupplierId());
                    supplier = productServiceClient.getSupplier(exp.getSupplierId());
                    if (supplier != null) {
                        supplierCache.put(exp.getSupplierId(), supplier);
                    }
                } else {
                    System.out.println("✅ Using CACHED supplier: supplier_id=" + exp.getSupplierId());
                }

                if (supplier != null) {
                    dto.setSupplierName(supplier.getName());
                    dto.setSupplierCode(supplier.getCode());
                    dto.setSupplierPhone(supplier.getPhone());
                    dto.setSupplierAddress(supplier.getAddress());
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to load supplier name: " + e.getMessage());
                dto.setSupplierName(null);
            }
        } else {
            dto.setSupplierName(null);
        }

        // ⭐ Load store name với CACHE
        if (exp.getStoreId() != null) {
            String storeName = storeCache.get(exp.getStoreId());
            if (storeName == null) {
                System.out.println("🔵 Loading store (NOT IN CACHE): store_id=" + exp.getStoreId());
                storeRepo.findById(exp.getStoreId()).ifPresent(store -> {
                    storeCache.put(exp.getStoreId(), store.getName());
                    dto.setStoreName(store.getName());
                });
            } else {
                System.out.println("✅ Using CACHED store: store_id=" + exp.getStoreId());
                dto.setStoreName(storeName);
            }
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
