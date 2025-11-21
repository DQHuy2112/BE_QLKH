package com.example.inventory_service.service;

import com.example.inventory_service.dto.StockDto;

import java.util.List;

public interface StockService {

    // tồn kho tất cả sản phẩm
    List<StockDto> getAllStock();

    // tồn kho theo 1 productId
    StockDto getStockByProduct(Long productId);

    // Kiểm tra tồn kho có đủ để xuất không
    boolean hasEnoughStock(Long productId, int quantity);

    // Lấy số lượng tồn hiện tại
    int getCurrentStock(Long productId);

}
