package com.example.inventory_service.service.impl;

import com.example.inventory_service.client.ProductServiceClient;
import com.example.inventory_service.dto.ImportDetailDto;
import com.example.inventory_service.dto.ImportDetailRequest;
import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;
import com.example.inventory_service.entity.ShopImport;
import com.example.inventory_service.entity.ShopImportDetail;
import com.example.inventory_service.exception.NotFoundException;
import com.example.inventory_service.repository.ShopImportDetailRepository;
import com.example.inventory_service.repository.ShopImportRepository;
import com.example.inventory_service.repository.ShopStockRepository;
import com.example.inventory_service.entity.ShopStock;
import com.example.inventory_service.service.ImportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ImportServiceImpl implements ImportService {

    private final ShopImportRepository importRepo;
    private final ShopImportDetailRepository detailRepo;
    private final ProductServiceClient productClient;
    private final com.example.inventory_service.repository.ShopStoreRepository storeRepo;
    private final ShopStockRepository stockRepo;
    private com.example.inventory_service.repository.UserQueryRepository userRepo;

    public ImportServiceImpl(
            ShopImportRepository importRepo,
            ShopImportDetailRepository detailRepo,
            ProductServiceClient productClient,
            com.example.inventory_service.repository.ShopStoreRepository storeRepo,
            ShopStockRepository stockRepo,
            com.example.inventory_service.repository.UserQueryRepository userRepo) {
        this.importRepo = importRepo;
        this.detailRepo = detailRepo;
        this.productClient = productClient;
        this.storeRepo = storeRepo;
        this.stockRepo = stockRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public SupplierImportDto create(SupplierImportRequest request) {
        // Validation: Phiếu nhập bắt buộc phải có kho và nhà cung cấp
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("Phiếu nhập kho bắt buộc phải có kho nhập");
        }
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("Phiếu nhập kho bắt buộc phải có nhà cung cấp");
        }

        java.util.Date now = new java.util.Date();

        ShopImport im = new ShopImport();

        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        } else {
            im.setCode("PNNCC" + System.currentTimeMillis());
        }

        // Lấy supplier type để set vào importType
        String importType = "SUPPLIER"; // Default
        try {
            var supplierInfo = productClient.getSupplier(request.getSupplierId());
            if (supplierInfo != null && supplierInfo.getType() != null) {
                importType = supplierInfo.getType(); // NCC, INTERNAL, STAFF, ...
            }
        } catch (Exception ex) {
            System.err.println("⚠️ Failed to get supplier type, using default SUPPLIER: " + ex.getMessage());
        }
        im.setImportType(importType);
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getSupplierId());
        im.setOrderId(request.getOrderId());

        im.setNote(limitNote(request.getNote()));
        im.setDescription(request.getDescription());
        im.setStatus("PENDING");
        im.setImportsDate(now);
        im.setCreatedAt(now);
        im.setUpdatedAt(now);
        
        // Set createdBy từ userId nếu có
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            im.setCreatedBy(currentUserId);
        } else if (im.getUserId() != null) {
            im.setCreatedBy(im.getUserId());
        }

        // Lưu ảnh
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

        // Lưu chi tiết
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
                // Nếu item có storeId thì dùng, không thì dùng storeId từ header
                d.setStoreId(item.getStoreId() != null ? item.getStoreId() : im.getStoreId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO);

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));

                // Áp dụng chiết khấu nếu có
                if (item.getDiscountPercent() != null && item.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountMultiplier = BigDecimal.ONE
                            .subtract(item.getDiscountPercent().divide(BigDecimal.valueOf(100), 4,
                                    java.math.RoundingMode.HALF_UP));
                    line = line.multiply(discountMultiplier);
                }

                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(im, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierImportDto> search(String status, String code, LocalDate from, LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        // Sử dụng phương thức search gộp tất cả loại
        List<ShopImport> list = importRepo.searchAllImports(
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

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierImportDto> searchPaged(String status,
                                               String code,
                                               LocalDate from,
                                               LocalDate to,
                                               String sortField,
                                               String sortDir,
                                               Pageable pageable) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        // Lấy toàn bộ danh sách đã lọc theo status, code, ngày
        List<ShopImport> imports = importRepo.searchAllImports(
                status,
                code,
                fromDate,
                toDate
        );

        // Sort theo ngày hoặc giá trị
        Comparator<ShopImport> comparator;
        if ("value".equalsIgnoreCase(sortField)) {
            // Tính totalValue từ details để sort
            comparator = Comparator.comparing(
                    (ShopImport im) -> {
                        List<ShopImportDetail> details = detailRepo.findByImportId(im.getId());
                        BigDecimal total = BigDecimal.ZERO;
                        if (details != null) {
                            for (ShopImportDetail d : details) {
                                if (d.getUnitPrice() == null || d.getQuantity() == null) continue;
                                BigDecimal line = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
                                if (d.getDiscountPercent() != null && d.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal discountMultiplier = BigDecimal.ONE
                                            .subtract(d.getDiscountPercent().divide(BigDecimal.valueOf(100), 4,
                                                    java.math.RoundingMode.HALF_UP));
                                    line = line.multiply(discountMultiplier);
                                }
                                total = total.add(line);
                            }
                        }
                        return total;
                    },
                    Comparator.nullsFirst(BigDecimal::compareTo)
            );
        } else {
            // Mặc định sort theo ngày nhập
            comparator = Comparator.comparing(
                    (ShopImport im) -> im.getImportsDate() != null ? im.getImportsDate().getTime() : 0L,
                    Comparator.nullsFirst(Long::compareTo)
            );
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        List<ShopImport> sorted = imports.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<ShopImport> pageContent;
        if (startItem >= sorted.size()) {
            pageContent = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, sorted.size());
            pageContent = sorted.subList(startItem, toIndex);
        }

        List<SupplierImportDto> dtoPage = pageContent.stream()
                .map(this::toDtoWithCalcTotal)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoPage, pageable, sorted.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierImportDto getById(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));
        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto update(Long id, SupplierImportRequest request) {
        // Validation: Phiếu nhập bắt buộc phải có kho và nhà cung cấp
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("Phiếu nhập kho bắt buộc phải có kho nhập");
        }
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("Phiếu nhập kho bắt buộc phải có nhà cung cấp");
        }

        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        // Lấy supplier type để set vào importType
        String importType = im.getImportType(); // Giữ nguyên nếu đã có
        if (request.getSupplierId() != null) {
            try {
                var supplierInfo = productClient.getSupplier(request.getSupplierId());
                if (supplierInfo != null && supplierInfo.getType() != null) {
                    importType = supplierInfo.getType(); // NCC, INTERNAL, STAFF, ...
                }
            } catch (Exception ex) {
                System.err.println("⚠️ Failed to get supplier type: " + ex.getMessage());
            }
        }
        im.setImportType(importType);

        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        }
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getSupplierId());
        im.setOrderId(request.getOrderId());
        im.setOrderId(request.getOrderId());

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
                // Nếu item có storeId thì dùng, không thì dùng storeId từ header
                d.setStoreId(item.getStoreId() != null ? item.getStoreId() : im.getStoreId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO);

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));

                // Áp dụng chiết khấu nếu có
                if (item.getDiscountPercent() != null && item.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountMultiplier = BigDecimal.ONE
                            .subtract(item.getDiscountPercent().divide(BigDecimal.valueOf(100), 4,
                                    java.math.RoundingMode.HALF_UP));
                    line = line.multiply(discountMultiplier);
                }

                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(im, total);
    }

    @Override
    @Transactional
    public SupplierImportDto approve(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể duyệt phiếu đang ở trạng thái PENDING");
        }

        im.setStatus("APPROVED");
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            im.setApprovedBy(currentUserId);
            im.setApprovedAt(new java.util.Date());
        }
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto confirm(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        if (!"APPROVED".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể nhập kho khi phiếu đã được duyệt (APPROVED)");
        }

        im.setStatus("IMPORTED");
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            im.setImportedBy(currentUserId);
            im.setImportedAt(new java.util.Date());
        }
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        // Cập nhật tồn kho vào shop_stocks
        List<ShopImportDetail> details = detailRepo.findByImportId(id);
        for (ShopImportDetail d : details) {
            if (d.getQuantity() != null && d.getQuantity() > 0 && d.getStoreId() != null) {
                // Lấy storeId từ detail (mỗi dòng có thể khác kho)
                Long storeId = d.getStoreId();
                // Tìm hoặc tạo stock record
                ShopStock stock = stockRepo.findByProductIdAndStoreId(d.getProductId(), storeId)
                        .orElseGet(() -> {
                            ShopStock newStock = new ShopStock();
                            newStock.setProductId(d.getProductId());
                            newStock.setStoreId(storeId);
                            newStock.setQuantity(0);
                            // Nếu sản phẩm chưa có trong kho, set mặc định minStock = 10 và maxStock = 1000
                            newStock.setMinStock(10);
                            newStock.setMaxStock(1000);
                            return stockRepo.save(newStock);
                        });

                // Tăng số lượng
                stock.setQuantity(stock.getQuantity() + d.getQuantity());
                stockRepo.save(stock);
            }
        }

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto cancel(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy phiếu đang ở trạng thái PENDING");
        }

        im.setStatus("CANCELLED");
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto reject(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể từ chối phiếu đang ở trạng thái PENDING");
        }

        im.setStatus("REJECTED");
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            im.setRejectedBy(currentUserId);
            im.setRejectedAt(new java.util.Date());
        }
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierImportDto> getAll() {
        return importRepo.findAll().stream()
                .map(this::toDtoWithCalcTotal)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierImportDto> getByStore(Long storeId) {
        return importRepo.findByStoreId(storeId).stream()
                .map(this::toDtoWithCalcTotal)
                .toList();
    }

    // ========= HELPER METHODS ========= //
    
    /**
     * Lấy userId hiện tại từ SecurityContext (username) và query từ database
     */
    private Long getCurrentUserId() {
        try {
            if (userRepo == null) {
                System.err.println("⚠️ userRepo is null in getCurrentUserId");
                return null;
            }
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                String username = auth.getName();
                System.out.println("🔍 Getting userId for username: " + username);
                java.util.Optional<Long> userIdOpt = userRepo.findUserIdByUsername(username);
                if (userIdOpt.isPresent()) {
                    System.out.println("✅ Found userId: " + userIdOpt.get() + " for username: " + username);
                    return userIdOpt.get();
                } else {
                    System.out.println("⚠️ No userId found for username: " + username);
                }
            } else {
                System.err.println("⚠️ No authentication found in SecurityContext");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get current userId: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Lấy tên đầy đủ của user từ username
     */
    private String getUserFullName(String username) {
        try {
            return userRepo.findFullNameByUsername(username)
                    .map(name -> name.trim())
                    .filter(name -> !name.isEmpty())
                    .orElse(username);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get user full name: " + e.getMessage());
            return username;
        }
    }
    
    /**
     * Lấy username hiện tại từ SecurityContext
     */
    private String getCurrentUsername() {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return auth.getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
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

    private SupplierImportDto toDtoWithCalcTotal(ShopImport im) {
        List<ShopImportDetail> details = detailRepo.findByImportId(im.getId());
        BigDecimal total = BigDecimal.ZERO;
        List<ImportDetailDto> itemDtos = new ArrayList<>();

        if (details != null) {
            for (ShopImportDetail d : details) {
                if (d.getUnitPrice() == null || d.getQuantity() == null)
                    continue;

                BigDecimal line = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));

                // Áp dụng chiết khấu nếu có
                if (d.getDiscountPercent() != null && d.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountMultiplier = BigDecimal.ONE
                            .subtract(d.getDiscountPercent().divide(BigDecimal.valueOf(100), 4,
                                    java.math.RoundingMode.HALF_UP));
                    line = line.multiply(discountMultiplier);
                }

                total = total.add(line);

                ImportDetailDto itemDto = new ImportDetailDto();
                itemDto.setId(d.getId());
                itemDto.setProductId(d.getProductId());
                itemDto.setStoreId(d.getStoreId());
                itemDto.setQuantity(d.getQuantity());
                itemDto.setUnitPrice(d.getUnitPrice());
                itemDto.setDiscountPercent(d.getDiscountPercent());
                itemDto.setProductCode(null);
                itemDto.setProductName(null);
                itemDto.setUnit(null);
                itemDto.setStoreName(null);
                itemDto.setStoreCode(null);

                // Lấy thông tin kho nếu có
                if (d.getStoreId() != null) {
                    storeRepo.findById(d.getStoreId()).ifPresent(store -> {
                        itemDto.setStoreName(store.getName());
                        itemDto.setStoreCode(store.getCode());
                    });
                }

                itemDtos.add(itemDto);
            }
        }

        SupplierImportDto dto = toDto(im, total);
        dto.setItems(itemDtos);
        return dto;
    }

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

        // Lấy thông tin kho đích
        if (imp.getStoreId() != null) {
            storeRepo.findById(imp.getStoreId()).ifPresent(store -> {
                dto.setStoreName(store.getName());
                dto.setStoreCode(store.getCode());
            });
        }

        // Phiếu nhập chỉ làm việc với NCC
        if (imp.getSupplierId() != null) {
            try {
                var supplierInfo = productClient.getSupplier(imp.getSupplierId());
                if (supplierInfo != null) {
                    dto.setSupplierName(supplierInfo.getName());
                    dto.setSupplierCode(supplierInfo.getCode());
                    dto.setSupplierPhone(supplierInfo.getPhone());
                    dto.setSupplierAddress(supplierInfo.getAddress());
                }
            } catch (Exception ex) {
                System.err.println("❌ Failed to get supplier info for supplierId: " + imp.getSupplierId());
                dto.setSupplierName(null);
            }
        }

        // Map ảnh
        List<String> images = new ArrayList<>();
        String raw = imp.getAttachmentImage();
        if (raw != null && !raw.isBlank()) {
            images = Arrays.stream(raw.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        dto.setAttachmentImages(images);

        // Map audit fields với userId và timestamp
        dto.setCreatedBy(imp.getCreatedBy());
        dto.setCreatedAt(imp.getCreatedAt());
        dto.setApprovedBy(imp.getApprovedBy());
        dto.setApprovedAt(imp.getApprovedAt());
        dto.setRejectedBy(imp.getRejectedBy());
        dto.setRejectedAt(imp.getRejectedAt());
        dto.setImportedBy(imp.getImportedBy());
        dto.setImportedAt(imp.getImportedAt());
        
        // Lấy tên user và role từ userId
        try {
            if (userRepo == null) {
                System.err.println("⚠️ userRepo is null, cannot fetch user names");
            } else {
                if (imp.getCreatedBy() != null) {
                    String createdByUsername = getUserFullNameFromId(imp.getCreatedBy());
                    if (createdByUsername != null && !createdByUsername.trim().isEmpty()) {
                        dto.setCreatedByName(createdByUsername);
                        System.out.println("✅ Set createdByName: " + createdByUsername + " for userId: " + imp.getCreatedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for createdBy userId: " + imp.getCreatedBy());
                    }
                    String createdByRole = getUserRoleFromId(imp.getCreatedBy());
                    if (createdByRole != null && !createdByRole.trim().isEmpty()) {
                        dto.setCreatedByRole(createdByRole);
                        System.out.println("✅ Set createdByRole: " + createdByRole + " for userId: " + imp.getCreatedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for createdBy userId: " + imp.getCreatedBy());
                    }
                }
                if (imp.getApprovedBy() != null) {
                    String approvedByUsername = getUserFullNameFromId(imp.getApprovedBy());
                    if (approvedByUsername != null && !approvedByUsername.trim().isEmpty()) {
                        dto.setApprovedByName(approvedByUsername);
                        System.out.println("✅ Set approvedByName: " + approvedByUsername + " for userId: " + imp.getApprovedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for approvedBy userId: " + imp.getApprovedBy());
                    }
                    String approvedByRole = getUserRoleFromId(imp.getApprovedBy());
                    if (approvedByRole != null && !approvedByRole.trim().isEmpty()) {
                        dto.setApprovedByRole(approvedByRole);
                        System.out.println("✅ Set approvedByRole: " + approvedByRole + " for userId: " + imp.getApprovedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for approvedBy userId: " + imp.getApprovedBy());
                    }
                }
                if (imp.getRejectedBy() != null) {
                    String rejectedByUsername = getUserFullNameFromId(imp.getRejectedBy());
                    if (rejectedByUsername != null && !rejectedByUsername.trim().isEmpty()) {
                        dto.setRejectedByName(rejectedByUsername);
                        System.out.println("✅ Set rejectedByName: " + rejectedByUsername + " for userId: " + imp.getRejectedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for rejectedBy userId: " + imp.getRejectedBy());
                    }
                    String rejectedByRole = getUserRoleFromId(imp.getRejectedBy());
                    if (rejectedByRole != null && !rejectedByRole.trim().isEmpty()) {
                        dto.setRejectedByRole(rejectedByRole);
                        System.out.println("✅ Set rejectedByRole: " + rejectedByRole + " for userId: " + imp.getRejectedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for rejectedBy userId: " + imp.getRejectedBy());
                    }
                }
                if (imp.getImportedBy() != null) {
                    String importedByUsername = getUserFullNameFromId(imp.getImportedBy());
                    if (importedByUsername != null && !importedByUsername.trim().isEmpty()) {
                        dto.setImportedByName(importedByUsername);
                        System.out.println("✅ Set importedByName: " + importedByUsername + " for userId: " + imp.getImportedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for importedBy userId: " + imp.getImportedBy());
                    }
                    String importedByRole = getUserRoleFromId(imp.getImportedBy());
                    if (importedByRole != null && !importedByRole.trim().isEmpty()) {
                        dto.setImportedByRole(importedByRole);
                        System.out.println("✅ Set importedByRole: " + importedByRole + " for userId: " + imp.getImportedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for importedBy userId: " + imp.getImportedBy());
                    }
                }
            }
        } catch (Exception e) {
            // Nếu có lỗi khi lấy user name, bỏ qua và tiếp tục
            System.err.println("⚠️ Failed to get user names: " + e.getMessage());
            e.printStackTrace();
        }

        return dto;
    }
    
    /**
     * Lấy fullName từ userId bằng cách query database
     */
    private String getUserFullNameFromId(Long userId) {
        try {
            if (userRepo == null) {
                return null;
            }
            Optional<String> fullName = userRepo.findFullNameByUserId(userId);
            if (fullName.isPresent() && !fullName.get().trim().isEmpty()) {
                return fullName.get().trim();
            }
            // Nếu không có fullName, lấy username
            Optional<String> username = userRepo.findUsernameByUserId(userId);
            return username.orElse(null);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get user full name from userId " + userId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Lấy role từ userId bằng cách query database
     */
    private String getUserRoleFromId(Long userId) {
        try {
            if (userRepo == null) {
                return null;
            }
            Optional<String> role = userRepo.findRoleByUserId(userId);
            return role.orElse(null);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get user role from userId " + userId + ": " + e.getMessage());
            return null;
        }
    }
}
