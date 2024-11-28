package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryState {
    private Long inventoryID;
    private Long supplierID;
    private String supplierName;
    private int maxAvailableSpace;
    private int usedSpace;
    // Mapping each ProductSchema to Map of ProductStatus to count
    private Map<Long, Map<ProductStatus, Long>> productStats;
    // Mapping ProductSchemaID to List of ProductState
    private Map<Long, List<ProductState>> stock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
