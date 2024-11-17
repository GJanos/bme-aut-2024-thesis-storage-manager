package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InventoryEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventListener.class);
    private final InventoryService inventoryService;

    public InventoryEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventListener
    @Async
    public void handleReorderEvent(ReorderAlertEvent event) {
        Inventory inventory = event.getInventory();
        Product product = event.getProduct();
        logger.info("Handling reorder event for product name: {} in inventory ID: {}", product.getSchema().getName(), inventory.getId());

        MyUser user = inventory.getSupplier().getUser();
        CreateProductStockRequest request = CreateProductStockRequest.builder().productSchemaId(product.getSchema().getId())
                .description(product.getDescription())
                .quantity(inventory.getReorderQuantity())
                .expiresAt(product.getExpiresAt())
                .build();

        inventoryService.addStock(user, request);
    }

    @EventListener
    @Async
    public void handleLowStockAlertEvent(LowStockAlertEvent event) {
        Inventory inventory = event.getInventory();
        Product product = event.getProduct();
        logger.info("Handling low stock alert event for product ID: {} in inventory ID: {}", product.getId(), inventory.getId());
        // Add logic to handle low stock alert, e.g., notifying the supplier or sending an alert email
    }

    @EventListener
    @Async
    public void handleProductExpiredEvent(ProductExpiredAlertEvent event) {
        Inventory inventory = event.getInventory();
        Product product = event.getProduct();
        logger.info("Handling product expired event for product ID: {} in inventory ID: {}", product.getId(), inventory.getId());
        // Add logic to handle low stock alert, e.g., notifying the supplier or sending an alert email
    }
}