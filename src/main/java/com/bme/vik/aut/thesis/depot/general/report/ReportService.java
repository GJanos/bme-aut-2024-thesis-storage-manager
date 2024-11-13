package com.bme.vik.aut.thesis.depot.general.report;

import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryState;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryStateReportResponse;
import com.bme.vik.aut.thesis.depot.general.report.dto.ProductState;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    @Value("${custom.inventory.max-depot-space}")
    private int MAX_AVAILABLE_DEPOT_SPACE;

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryStateReportResponse getInventoryStateReportResponse() {
        List<Inventory> inventories = inventoryRepository.findAll();

        int totalUsedSpace = inventories.stream().mapToInt(Inventory::getUsedSpace).sum();

        List<InventoryState> inventoryStates = inventories.stream()
                .map(this::mapToInventoryState)
                .collect(Collectors.toList());

        return InventoryStateReportResponse.builder()
                .maxAvailableSpaceInStorage(MAX_AVAILABLE_DEPOT_SPACE)
                .usedSpaceInStorage(totalUsedSpace)
                .numOfInventories(inventories.size())
                .inventoryStates(inventoryStates)
                .build();
    }

    private InventoryState mapToInventoryState(Inventory inventory) {
        Map<Long, List<ProductState>> stock = inventory.getProductIds().stream()
                .collect(Collectors.groupingBy(
                        productId -> productRepository.findById(productId).orElseThrow().getSchema().getId(),
                        Collectors.mapping(this::mapToProductState, Collectors.toList())
                ));

        return InventoryState.builder()
                .inventoryID(inventory.getId())
                .supplierID(inventory.getSupplier().getId())
                .supplierName(inventory.getSupplier().getName())
                .maxAvailableSpace(inventory.getMaxAvailableSpace())
                .usedSpace(inventory.getUsedSpace())
                .stock(stock)
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private ProductState mapToProductState(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + productId + " not found"));

        return ProductState.builder()
                .productID(product.getId())
                .productSchemaID(product.getSchema().getId())
                .productName(product.getSchema().getName())
                .status(product.getStatus())
                .storageSpaceNeeded(product.getSchema().getStorageSpaceNeeded())
                .categoryIDs(product.getSchema().getCategories().stream().map(Category::getId).collect(Collectors.toList()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

