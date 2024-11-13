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
public class ProductState {
    private Long productID;
    private Long productSchemaID;
    private String productName;
    private ProductStatus status;
    private int storageSpaceNeeded;
    private List<Long> categoryIDs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
