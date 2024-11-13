package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryStateReportResponse {
    private int maxAvailableSpaceInStorage;
    private int usedSpaceInStorage;
    private int numOfInventories;
    private List<InventoryState> inventoryStates;
}
