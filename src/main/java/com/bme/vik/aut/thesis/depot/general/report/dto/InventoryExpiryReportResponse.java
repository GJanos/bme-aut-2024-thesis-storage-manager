package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class InventoryExpiryReportResponse {
    private Map<ExpiryStatus, Integer> depotExpiryStats;
    private List<InventoryExpiry> inventoryExpires;
}
