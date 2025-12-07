package com.example.inventory_service.service.impl;

import com.example.inventory_service.dto.ExportDetailDto;
import com.example.inventory_service.dto.ExportDetailRequest;
import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.entity.ShopExport;
import com.example.inventory_service.entity.ShopExportDetail;
import com.example.inventory_service.exception.NotFoundException;
import com.example.inventory_service.entity.ShopStock;
import com.example.inventory_service.repository.ShopExportDetailRepository;
import com.example.inventory_service.repository.ShopExportRepository;
import com.example.inventory_service.repository.ShopStockRepository;
import com.example.inventory_service.service.ExportService;
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
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    private final ShopExportRepository exportRepo;
    private final ShopExportDetailRepository detailRepo;
    private final com.example.inventory_service.repository.ShopStoreRepository storeRepo;
    private final ShopStockRepository stockRepo;
    private com.example.inventory_service.repository.UserQueryRepository userRepo;

    public ExportServiceImpl(
            ShopExportRepository exportRepo,
            ShopExportDetailRepository detailRepo,
            com.example.inventory_service.repository.ShopStoreRepository storeRepo,
            ShopStockRepository stockRepo,
            com.example.inventory_service.repository.UserQueryRepository userRepo) {
        this.exportRepo = exportRepo;
        this.detailRepo = detailRepo;
        this.storeRepo = storeRepo;
        this.stockRepo = stockRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public SupplierExportDto create(SupplierExportRequest req) {
        // Validation: Phiếu xuất bắt buộc phải có kho và khách hàng
        // Lấy storeId từ header hoặc từ item đầu tiên
        Long storeId = req.getStoreId();
        if (storeId == null && req.getItems() != null && !req.getItems().isEmpty()) {
            // Nếu header không có storeId, lấy từ item đầu tiên
            ExportDetailRequest firstItem = req.getItems().get(0);
            if (firstItem.getStoreId() != null) {
                storeId = firstItem.getStoreId();
            }
        }
        if (storeId == null) {
            throw new IllegalArgumentException("Phiếu xuất kho bắt buộc phải có kho xuất");
        }
        if (req.getCustomerId() == null &&
                (req.getCustomerName() == null || req.getCustomerName().isBlank())) {
            throw new IllegalArgumentException("Phiếu xuất kho bắt buộc phải có thông tin khách hàng");
        }

        java.util.Date now = new java.util.Date();

        ShopExport export = new ShopExport();
        export.setCode(req.getCode() != null ? req.getCode() : "PXNCC" + System.currentTimeMillis());
        export.setExportType("ORDER"); // Cố định = ORDER
        export.setStoreId(storeId);

        export.setNote(req.getNote());
        export.setDescription(req.getDescription());
        export.setCustomerId(req.getCustomerId());
        // Lưu thông tin khách hàng nếu có (nhập trực tiếp)
        export.setCustomerName(req.getCustomerName());
        export.setCustomerPhone(req.getCustomerPhone());
        export.setCustomerAddress(req.getCustomerAddress());
        export.setStatus("PENDING");
        export.setExportsDate(now);
        export.setUserId(null);
        export.setOrderId(req.getOrderId());
        export.setCreatedAt(now);
        export.setUpdatedAt(now);
        
        // Set createdBy từ userId nếu có
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            export.setCreatedBy(currentUserId);
        } else if (export.getUserId() != null) {
            export.setCreatedBy(export.getUserId());
        }

        // Lưu ảnh
        if (req.getAttachmentImages() != null && !req.getAttachmentImages().isEmpty()) {
            String joined = req.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            export.setAttachmentImage(joined);
        } else {
            export.setAttachmentImage(null);
        }

        export = exportRepo.save(export);

        // Chi tiết phiếu xuất
        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        if (req.getItems() != null) {
            for (ExportDetailRequest item : req.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    continue;
                }
                if (item.getUnitPrice() == null) {
                    continue;
                }

                ShopExportDetail d = new ShopExportDetail();
                d.setExportId(export.getId());
                d.setProductId(item.getProductId());
                // Nếu item có storeId thì dùng, không thì dùng storeId từ header
                d.setStoreId(item.getStoreId() != null ? item.getStoreId() : export.getStoreId());
                d.setImportDetailsId(item.getImportDetailsId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent());

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

                details.add(d);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(export, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierExportDto> search(String status, String code, LocalDate from, LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        // Sử dụng phương thức search gộp tất cả loại
        List<ShopExport> list = exportRepo.searchAllExports(
                status,
                code,
                fromDate,
                toDate);

        List<SupplierExportDto> result = new ArrayList<>();
        for (ShopExport e : list) {
            result.add(toDtoWithCalcTotal(e));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierExportDto> searchPaged(String status,
                                               String code,
                                               LocalDate from,
                                               LocalDate to,
                                               String sortField,
                                               String sortDir,
                                               Pageable pageable) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        // Lấy toàn bộ danh sách đã lọc theo status, code, ngày
        List<ShopExport> exports = exportRepo.searchAllExports(
                status,
                code,
                fromDate,
                toDate
        );

        // Sort theo ngày hoặc giá trị
        Comparator<ShopExport> comparator;
        if ("value".equalsIgnoreCase(sortField)) {
            // Tính totalValue từ details để sort
            comparator = Comparator.comparing(
                    (ShopExport e) -> {
                        List<ShopExportDetail> details = detailRepo.findByExportId(e.getId());
                        BigDecimal total = BigDecimal.ZERO;
                        if (details != null) {
                            for (ShopExportDetail d : details) {
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
            // Mặc định sort theo ngày xuất
            comparator = Comparator.comparing(
                    (ShopExport e) -> e.getExportsDate() != null ? e.getExportsDate().getTime() : 0L,
                    Comparator.nullsFirst(Long::compareTo)
            );
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        List<ShopExport> sorted = exports.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<ShopExport> pageContent;
        if (startItem >= sorted.size()) {
            pageContent = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, sorted.size());
            pageContent = sorted.subList(startItem, toIndex);
        }

        List<SupplierExportDto> dtoPage = pageContent.stream()
                .map(this::toDtoWithCalcTotal)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoPage, pageable, sorted.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierExportDto getById(Long id) {
        ShopExport e = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));
        return toDtoWithCalcTotal(e);
    }

    @Override
    @Transactional
    public SupplierExportDto update(Long id, SupplierExportRequest req) {
        // Validation: Phiếu xuất bắt buộc phải có kho và khách hàng
        if (req.getStoreId() == null) {
            throw new IllegalArgumentException("Phiếu xuất kho bắt buộc phải có kho xuất");
        }
        if (req.getCustomerId() == null &&
                (req.getCustomerName() == null || req.getCustomerName().isBlank())) {
            throw new IllegalArgumentException("Phiếu xuất kho bắt buộc phải có thông tin khách hàng");
        }

        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        export.setExportType("ORDER"); // Cố định = ORDER

        if (req.getCode() != null && !req.getCode().isBlank()) {
            export.setCode(req.getCode());
        }
        export.setStoreId(req.getStoreId());
        export.setCustomerId(req.getCustomerId());
        // Lưu thông tin khách hàng nếu có (nhập trực tiếp)
        export.setCustomerName(req.getCustomerName());
        export.setCustomerPhone(req.getCustomerPhone());
        export.setCustomerAddress(req.getCustomerAddress());
        export.setOrderId(req.getOrderId());
        export.setNote(req.getNote());
        export.setDescription(req.getDescription());
        export.setUpdatedAt(new java.util.Date());

        // Cập nhật ảnh
        if (req.getAttachmentImages() != null && !req.getAttachmentImages().isEmpty()) {
            String joined = req.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            export.setAttachmentImage(joined);
        } else {
            export.setAttachmentImage(null);
        }

        export = exportRepo.save(export);

        // Xóa chi tiết cũ
        detailRepo.deleteByExportId(id);

        // Tạo chi tiết mới
        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        if (req.getItems() != null) {
            for (ExportDetailRequest item : req.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    continue;
                }
                if (item.getUnitPrice() == null) {
                    continue;
                }

                ShopExportDetail d = new ShopExportDetail();
                d.setExportId(export.getId());
                d.setProductId(item.getProductId());
                // Nếu item có storeId thì dùng, không thì dùng storeId từ header
                d.setStoreId(item.getStoreId() != null ? item.getStoreId() : export.getStoreId());
                d.setImportDetailsId(item.getImportDetailsId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent());

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

                details.add(d);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(export, total);
    }

    @Override
    @Transactional
    public SupplierExportDto approve(Long id) {
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        if (!"PENDING".equals(export.getStatus())) {
            throw new IllegalStateException("Chỉ có thể duyệt phiếu đang ở trạng thái PENDING");
        }

        export.setStatus("APPROVED");
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            export.setApprovedBy(currentUserId);
            export.setApprovedAt(new java.util.Date());
        }
        export.setUpdatedAt(new java.util.Date());
        export = exportRepo.save(export);

        return toDtoWithCalcTotal(export);
    }

    @Override
    @Transactional
    public SupplierExportDto confirm(Long id) {
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        if (!"APPROVED".equals(export.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xuất kho khi phiếu đã được duyệt (APPROVED)");
        }

        // Kiểm tra và trừ tồn kho từ shop_stocks
        List<ShopExportDetail> details = detailRepo.findByExportId(id);

        // Kiểm tra tồn kho trước khi xuất (mỗi dòng có thể khác kho)
        for (ShopExportDetail d : details) {
            Integer quantity = d.getQuantity();
            if (quantity == null || quantity <= 0) {
                continue;
            }

            Long productId = d.getProductId();
            Long storeId = d.getStoreId(); // Lấy từ detail (mỗi dòng có thể khác kho)

            if (storeId == null) {
                throw new IllegalStateException(
                        String.format("Dòng sản phẩm ID %d không có kho xuất", productId));
            }

            // Kiểm tra tồn kho từ shop_stocks
            ShopStock stock = stockRepo.findByProductIdAndStoreId(productId, storeId)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("Không tìm thấy tồn kho cho sản phẩm ID %d tại kho ID %d",
                                    productId, storeId)));

            if (stock.getQuantity() < quantity) {
                throw new IllegalStateException(
                        String.format("Sản phẩm ID %d không đủ số lượng trong kho ID %d. Tồn: %d, Cần: %d",
                                productId, storeId, stock.getQuantity(), quantity));
            }
        }

        // Cập nhật trạng thái
        export.setStatus("EXPORTED");
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            export.setExportedBy(currentUserId);
            export.setExportedAt(new java.util.Date());
        }
        export.setUpdatedAt(new java.util.Date());
        export = exportRepo.save(export);

        // Trừ tồn kho từ shop_stocks (mỗi dòng trừ tại kho riêng)
        for (ShopExportDetail d : details) {
            if (d.getQuantity() != null && d.getQuantity() > 0 && d.getStoreId() != null) {
                Long storeId = d.getStoreId(); // Lấy từ detail
                ShopStock stock = stockRepo.findByProductIdAndStoreId(d.getProductId(), storeId)
                        .orElseThrow(() -> new IllegalStateException(
                                String.format("Không tìm thấy tồn kho cho sản phẩm ID %d tại kho ID %d",
                                        d.getProductId(), storeId)));

                stock.setQuantity(stock.getQuantity() - d.getQuantity());
                stockRepo.save(stock);
            }
        }

        return toDtoWithCalcTotal(export);
    }

    @Override
    @Transactional
    public SupplierExportDto cancel(Long id) {
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        if (!"PENDING".equals(export.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy phiếu đang ở trạng thái PENDING");
        }

        export.setStatus("CANCELLED");
        export.setUpdatedAt(new java.util.Date());
        export = exportRepo.save(export);

        return toDtoWithCalcTotal(export);
    }

    @Override
    @Transactional
    public SupplierExportDto reject(Long id) {
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        if (!"PENDING".equals(export.getStatus())) {
            throw new IllegalStateException("Chỉ có thể từ chối phiếu đang ở trạng thái PENDING");
        }

        export.setStatus("REJECTED");
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            export.setRejectedBy(currentUserId);
            export.setRejectedAt(new java.util.Date());
        }
        export.setUpdatedAt(new java.util.Date());
        export = exportRepo.save(export);

        return toDtoWithCalcTotal(export);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierExportDto> getAll() {
        return exportRepo.findAll().stream()
                .map(this::toDtoWithCalcTotal)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierExportDto> getByStore(Long storeId) {
        return exportRepo.findByStoreId(storeId).stream()
                .map(this::toDtoWithCalcTotal)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierExportDto> getByOrder(Long orderId) {
        return exportRepo.findByOrderId(orderId).stream()
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
     * Lấy fullName từ userId bằng cách query database
     */
    private String getUserFullNameFromId(Long userId) {
        try {
            if (userRepo == null) {
                return null;
            }
            java.util.Optional<String> fullName = userRepo.findFullNameByUserId(userId);
            if (fullName.isPresent() && !fullName.get().trim().isEmpty()) {
                return fullName.get().trim();
            }
            // Nếu không có fullName, lấy username
            java.util.Optional<String> username = userRepo.findUsernameByUserId(userId);
            return username.orElse(null);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get user full name from userId " + userId + ": " + e.getMessage());
            return null;
        }
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

    private SupplierExportDto toDtoWithCalcTotal(ShopExport e) {
        BigDecimal total = BigDecimal.ZERO;
        List<ExportDetailDto> itemDtos = new ArrayList<>();

        List<ShopExportDetail> details = detailRepo.findByExportId(e.getId());

        if (details != null) {
            for (ShopExportDetail d : details) {
                if (d.getUnitPrice() == null || d.getQuantity() == null)
                    continue;

                BigDecimal line = d.getUnitPrice()
                        .multiply(BigDecimal.valueOf(d.getQuantity()));

                // Áp dụng chiết khấu nếu có
                if (d.getDiscountPercent() != null && d.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountMultiplier = BigDecimal.ONE
                            .subtract(d.getDiscountPercent().divide(BigDecimal.valueOf(100), 4,
                                    java.math.RoundingMode.HALF_UP));
                    line = line.multiply(discountMultiplier);
                }

                total = total.add(line);

                ExportDetailDto itemDto = new ExportDetailDto();
                itemDto.setId(d.getId());
                itemDto.setProductId(d.getProductId());
                itemDto.setStoreId(d.getStoreId());
                itemDto.setQuantity(d.getQuantity());
                itemDto.setUnitPrice(d.getUnitPrice());
                itemDto.setDiscountPercent(d.getDiscountPercent());
                itemDto.setImportDetailsId(d.getImportDetailsId());
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

        SupplierExportDto dto = toDto(e, total);
        dto.setItems(itemDtos);
        return dto;
    }

    private SupplierExportDto toDto(ShopExport e, BigDecimal total) {
        SupplierExportDto dto = new SupplierExportDto();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setStoreId(e.getStoreId());
        dto.setCustomerId(e.getCustomerId());
        dto.setStatus(e.getStatus());
        dto.setExportsDate(e.getExportsDate());
        dto.setNote(e.getNote());
        dto.setTotalValue(total);

        // Lấy thông tin khách hàng từ entity (đã lưu khi tạo/cập nhật)
        dto.setCustomerName(e.getCustomerName());
        dto.setCustomerPhone(e.getCustomerPhone());
        dto.setCustomerAddress(e.getCustomerAddress());
        // TODO: Nếu customerId có và customerName chưa có, có thể fetch từ customer
        // service
        // if (e.getCustomerId() != null && e.getCustomerName() == null) {
        // // Fetch from customer service
        // }

        // Lấy thông tin kho
        if (e.getStoreId() != null) {
            storeRepo.findById(e.getStoreId()).ifPresent(store -> {
                dto.setStoreName(store.getName());
            });
        }

        // Map ảnh
        List<String> images = new ArrayList<>();
        String raw = e.getAttachmentImage();
        if (raw != null && !raw.isBlank()) {
            images = Arrays.stream(raw.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        dto.setAttachmentImages(images);

        // Map audit fields với userId và timestamp
        dto.setCreatedBy(e.getCreatedBy());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setApprovedBy(e.getApprovedBy());
        dto.setApprovedAt(e.getApprovedAt());
        dto.setRejectedBy(e.getRejectedBy());
        dto.setRejectedAt(e.getRejectedAt());
        dto.setExportedBy(e.getExportedBy());
        dto.setExportedAt(e.getExportedAt());
        
        // Lấy tên user và role từ userId
        try {
            if (userRepo == null) {
                System.err.println("⚠️ userRepo is null, cannot fetch user names");
            } else {
                if (e.getCreatedBy() != null) {
                    String createdByUsername = getUserFullNameFromId(e.getCreatedBy());
                    if (createdByUsername != null && !createdByUsername.trim().isEmpty()) {
                        dto.setCreatedByName(createdByUsername);
                        System.out.println("✅ Set createdByName: " + createdByUsername + " for userId: " + e.getCreatedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for createdBy userId: " + e.getCreatedBy());
                    }
                    String createdByRole = getUserRoleFromId(e.getCreatedBy());
                    if (createdByRole != null && !createdByRole.trim().isEmpty()) {
                        dto.setCreatedByRole(createdByRole);
                        System.out.println("✅ Set createdByRole: " + createdByRole + " for userId: " + e.getCreatedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for createdBy userId: " + e.getCreatedBy());
                    }
                }
                if (e.getApprovedBy() != null) {
                    String approvedByUsername = getUserFullNameFromId(e.getApprovedBy());
                    if (approvedByUsername != null && !approvedByUsername.trim().isEmpty()) {
                        dto.setApprovedByName(approvedByUsername);
                        System.out.println("✅ Set approvedByName: " + approvedByUsername + " for userId: " + e.getApprovedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for approvedBy userId: " + e.getApprovedBy());
                    }
                    String approvedByRole = getUserRoleFromId(e.getApprovedBy());
                    if (approvedByRole != null && !approvedByRole.trim().isEmpty()) {
                        dto.setApprovedByRole(approvedByRole);
                        System.out.println("✅ Set approvedByRole: " + approvedByRole + " for userId: " + e.getApprovedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for approvedBy userId: " + e.getApprovedBy());
                    }
                }
                if (e.getRejectedBy() != null) {
                    String rejectedByUsername = getUserFullNameFromId(e.getRejectedBy());
                    if (rejectedByUsername != null && !rejectedByUsername.trim().isEmpty()) {
                        dto.setRejectedByName(rejectedByUsername);
                        System.out.println("✅ Set rejectedByName: " + rejectedByUsername + " for userId: " + e.getRejectedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for rejectedBy userId: " + e.getRejectedBy());
                    }
                    String rejectedByRole = getUserRoleFromId(e.getRejectedBy());
                    if (rejectedByRole != null && !rejectedByRole.trim().isEmpty()) {
                        dto.setRejectedByRole(rejectedByRole);
                        System.out.println("✅ Set rejectedByRole: " + rejectedByRole + " for userId: " + e.getRejectedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for rejectedBy userId: " + e.getRejectedBy());
                    }
                }
                if (e.getExportedBy() != null) {
                    String exportedByUsername = getUserFullNameFromId(e.getExportedBy());
                    if (exportedByUsername != null && !exportedByUsername.trim().isEmpty()) {
                        dto.setExportedByName(exportedByUsername);
                        System.out.println("✅ Set exportedByName: " + exportedByUsername + " for userId: " + e.getExportedBy());
                    } else {
                        System.out.println("⚠️ Could not get name for exportedBy userId: " + e.getExportedBy());
                    }
                    String exportedByRole = getUserRoleFromId(e.getExportedBy());
                    if (exportedByRole != null && !exportedByRole.trim().isEmpty()) {
                        dto.setExportedByRole(exportedByRole);
                        System.out.println("✅ Set exportedByRole: " + exportedByRole + " for userId: " + e.getExportedBy());
                    } else {
                        System.out.println("⚠️ Could not get role for exportedBy userId: " + e.getExportedBy());
                    }
                }
            }
        } catch (Exception ex) {
            // Nếu có lỗi khi lấy user name, bỏ qua và tiếp tục
            System.err.println("⚠️ Failed to get user names: " + ex.getMessage());
            ex.printStackTrace();
        }

        return dto;
    }
    
    /**
     * Lấy role từ userId bằng cách query database
     */
    private String getUserRoleFromId(Long userId) {
        try {
            if (userRepo == null) {
                return null;
            }
            java.util.Optional<String> role = userRepo.findRoleByUserId(userId);
            return role.orElse(null);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get user role from userId " + userId + ": " + e.getMessage());
            return null;
        }
    }
}

