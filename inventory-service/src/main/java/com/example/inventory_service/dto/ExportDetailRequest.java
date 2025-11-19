package com.example.inventory_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExportDetailRequest {
    private Long productId;
    private Long importDetailsId;   // có thể null, sau này xử lý FIFO
    private Integer quantity;
    private BigDecimal unitPrice;
}
