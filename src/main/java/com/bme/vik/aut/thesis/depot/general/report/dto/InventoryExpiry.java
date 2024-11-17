package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InventoryExpiry {
    private Long inventoryID;
    private Long supplierID;
    private String supplierName;
    private Map<ExpiryStatus, List<ProductExpiry>> stock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}