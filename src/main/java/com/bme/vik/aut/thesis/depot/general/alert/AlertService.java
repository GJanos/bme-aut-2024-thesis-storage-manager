package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.*;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    @Value("${custom.alert.auto-reorder}")
    private boolean AUTO_REORDER_ENABLED;

    @Value("${custom.alert.low-stock}")
    private boolean LOW_STOCK_ALERT_ENABLED;

    @Value("${custom.alert.expiration}")
    private boolean EXPIRY_ALERT_ENABLED;

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeService timeService;
    private final ProductService productService;
    private final ProductSchemaService productSchemaService;
    @Setter
    private InventoryService inventoryService;

    private final Queue<Runnable> eventQueue = new LinkedList<>();

    @Scheduled(fixedRateString = "${custom.alert.expiry-check-interval-ms}", initialDelay = 10000)
    @Transactional
    public void checkForExpiredProducts() {
        if (!EXPIRY_ALERT_ENABLED) {
            logger.info("Expiry alert is disabled. Skipping expiry check.");
            return;
        }

        logger.info("Running periodic expiry check");

        List<Inventory> inventories = inventoryRepository.findAll();

        for (Inventory inventory : inventories) {
            for (Long productId : inventory.getProductIds()) {
                Product product = productService.getProductById(productId);

                ExpiryStatus newExpiryStatus = determineExpiryStatus(product.getExpiresAt(), inventory.getExpiryAlertThreshold());
                ExpiryStatus oldExpiryStatus = product.getExpiryStatus();

                if (newExpiryStatus != oldExpiryStatus) {
                    logger.info("Expiry status changed for product ID: {} in inventory ID: {} from {} to {}",
                            productId, inventory.getId(), oldExpiryStatus, newExpiryStatus);

                    product.setExpiryStatus(newExpiryStatus);
                    inventoryService.changeProductExpirationStatus(inventory.getId(), product.getSchema().getId(), productId, newExpiryStatus);

                    eventQueue.add(() -> emitProductExpiredEvent(inventory, product));
                }
            }
        }
        logger.info("Expiry check completed.");
        processEventQueue();
    }

    public void checkStockForReorder(Map<Long, Map<Long, List<Product>>> stock) {
        logger.info("Checking all inventories in the depot for reorder and low stock alerts");

        stock.forEach((inventoryId, productSchemaMap) -> {
            Inventory inventory = inventoryService.getInventoryById(inventoryId);

            productSchemaMap.forEach((productSchemaId, products) -> {
                String productName = productSchemaService.getProductSchemaById(productSchemaId).getName();
                Long supplierId = inventory.getSupplier().getId();

                logger.info("Checking product schema ID: {} (Product: {}) for supplier ID: {}", productSchemaId, productName, supplierId);

                if (LOW_STOCK_ALERT_ENABLED && inventoryService.lowOnStock(inventory, productSchemaId)) {
                    Product product = products.get(0); // Just need a product for the event
                    eventQueue.add(() -> emitLowStockAlertEvent(inventory, product));
                }

                if (AUTO_REORDER_ENABLED && inventoryService.needsReorderForStock(inventory, productSchemaId)) {
                    Product product = products.get(0); // Just need a product for the event
                    eventQueue.add(() -> emitReorderEvent(inventory, product));
                }
            });
        });

        processEventQueue();
    }

    private void processEventQueue() {
        while (!eventQueue.isEmpty()) {
            eventQueue.poll().run(); // Execute each queued event
        }
    }

    public ExpiryStatus determineExpiryStatus(LocalDateTime expiryDate, int expiryAlertThreshold) {
        LocalDateTime now = timeService.getCurrentTime();
        if (expiryDate.isBefore(now.minusDays(30))) {
            return ExpiryStatus.LONGEXPIRED;
        } else if (expiryDate.isBefore(now)) {
            return ExpiryStatus.EXPIRED;
        } else if (expiryDate.isBefore(now.plusDays(expiryAlertThreshold))) {
            return ExpiryStatus.SOONTOEXPIRE;
        } else {
            return ExpiryStatus.NOTEXPIRED;
        }
    }

    private void emitReorderEvent(Inventory inventory, Product product) {
        logger.info("Emitting reorder event for product ID: {} in inventory ID: {}", product.getId(), inventory.getId());
        eventPublisher.publishEvent(new ReorderAlertEvent(this, inventory, product));
    }

    private void emitLowStockAlertEvent(Inventory inventory, Product product) {
        logger.info("Emitting low stock alert event for product ID: {} in inventory ID: {}", product.getId(), inventory.getId());
        eventPublisher.publishEvent(new LowStockAlertEvent(this, inventory, product));
    }

    private void emitProductExpiredEvent(Inventory inventory, Product product) {
        logger.info("Emitting product expired event for product ID: {} in inventory ID: {}", product.getId(), inventory.getId());
        eventPublisher.publishEvent(new ProductExpiredAlertEvent(this, inventory, product));
    }
}
