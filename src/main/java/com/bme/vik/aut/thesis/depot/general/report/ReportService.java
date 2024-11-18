package com.bme.vik.aut.thesis.depot.general.report;

import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;

import java.util.Optional;

import com.bme.vik.aut.thesis.depot.general.order.Order;
import com.bme.vik.aut.thesis.depot.general.order.OrderRepository;
import com.bme.vik.aut.thesis.depot.general.order.OrderStatus;
import com.bme.vik.aut.thesis.depot.general.report.dto.*;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
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
    private final OrderRepository orderRepository;

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

    public InventoryState mapToInventoryState(Inventory inventory) {
        Map<Long, List<ProductState>> stock = inventory.getProductIds().stream()
                .collect(Collectors.groupingBy(
                        productId -> productRepository.findById(productId).orElseThrow().getSchema().getId(),
                        Collectors.mapping(this::mapToProductState, Collectors.toList())
                ));

        // Mapping each ProductSchemaID to a map of ProductStatus to count
        Map<Long, Map<ProductStatus, Long>> productStats = inventory.getProductIds().stream()
                .map(productId -> productRepository.findById(productId).orElseThrow())
                .collect(Collectors.groupingBy(
                        product -> product.getSchema().getId(),
                        Collectors.groupingBy(
                                Product::getStatus,
                                Collectors.counting()
                        )
                ));

        return InventoryState.builder()
                .inventoryID(inventory.getId())
                .supplierID(inventory.getSupplier().getId())
                .supplierName(inventory.getSupplier().getName())
                .maxAvailableSpace(inventory.getMaxAvailableSpace())
                .usedSpace(inventory.getUsedSpace())
                .stock(stock)
                .productStats(productStats)
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private ProductState mapToProductState(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();

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

    private ProductState mapToProductState(Product product) {
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


    public InventoryExpiryReportResponse getInventoryExpiryReportResponse() {
        List<Inventory> inventories = inventoryRepository.findAll();

        List<InventoryExpiry> inventoryExpiries = inventories.stream()
                .map(this::mapToInventoryExpiry)
                .collect(Collectors.toList());

        // Count products by expiry status
        Map<ExpiryStatus, Integer> depotExpiryStats = new EnumMap<>(ExpiryStatus.class);
        inventoryExpiries.forEach(inventoryExpiry -> {
            inventoryExpiry.getStock().forEach((expiryStatus, products) ->
                    depotExpiryStats.merge(expiryStatus, products.size(), Integer::sum));
        });

        return InventoryExpiryReportResponse.builder()
                .depotExpiryStats(depotExpiryStats)
                .inventoryExpires(inventoryExpiries)
                .build();
    }

    private InventoryExpiry mapToInventoryExpiry(Inventory inventory) {
        Map<ExpiryStatus, List<ProductExpiry>> stock = inventory.getProductIds().stream()
                .map(productRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::mapToProductExpiry)
                .collect(Collectors.groupingBy(ProductExpiry::getExpiryStatus));

        return InventoryExpiry.builder()
                .inventoryID(inventory.getId())
                .supplierID(inventory.getSupplier().getId())
                .supplierName(inventory.getSupplier().getName())
                .stock(stock)
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private ProductExpiry mapToProductExpiry(Product product) {
        return ProductExpiry.builder()
                .productID(product.getId())
                .productName(product.getSchema().getName())
                .expiresAt(product.getExpiresAt())
                .expiryStatus(product.getExpiryStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public OrderReportResponse getOrderReportResponse() {
        List<Order> orders = orderRepository.findAll();

        // Count orders by status
        Map<OrderStatus, Long> orderStats = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        // Map orders to user-specific details
        List<UserOrder> userOrders = orders.stream()
                .map(this::mapToUserOrder)
                .collect(Collectors.toList());

        return OrderReportResponse.builder()
                .numOfOrders(orders.size())
                .orderStats(orderStats)
                .userOrders(userOrders)
                .build();
    }

    private UserOrder mapToUserOrder(Order order) {
        List<ProductState> products = order.getOrderItems().stream()
                .map(this::mapToProductState)
                .collect(Collectors.toList());

        return UserOrder.builder()
                .orderID(order.getId())
                .userID(order.getUserId())
                .orderStatus(order.getStatus())
                .products(products)
                .createdAt(order.getCreatedAt())
                .build();
    }
}

