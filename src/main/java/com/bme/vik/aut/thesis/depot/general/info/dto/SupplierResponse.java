package com.bme.vik.aut.thesis.depot.general.info.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long id;
    private String name;
    private Long inventoryId;
    private int usedSpace;
    private int maxAvailableSpace;
}
