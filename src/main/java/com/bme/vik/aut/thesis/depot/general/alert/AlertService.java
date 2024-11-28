package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
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
import java.util.*;

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
            List<Product> expiredProducts = new ArrayList<>();

            for (Long productId : inventory.getProductIds()) {
                Product product = productService.getProductById(productId);

                ExpiryStatus newExpiryStatus = determineExpiryStatus(product.getExpiresAt(), inventory.getExpiryAlertThreshold());
                ExpiryStatus oldExpiryStatus = product.getExpiryStatus();

                if (newExpiryStatus != oldExpiryStatus) {
                    logger.info("Expiry status changed for product ID: {} in inventory ID: {} from {} to {}",
                            productId, inventory.getId(), oldExpiryStatus, newExpiryStatus);

                    product.setExpiryStatus(newExpiryStatus);
                    inventoryService.changeProductExpirationStatus(inventory.getId(), product.getSchema().getId(), productId, newExpiryStatus);
                }

                if (newExpiryStatus != ExpiryStatus.NOTEXPIRED && product.getStatus() != ProductStatus.REMOVED) {
                    expiredProducts.add(product);
                }
            }

            if (!expiredProducts.isEmpty()) {
                eventPublisher.publishEvent(new ProductExpiredAlertEvent(this, inventory, expiredProducts));
            }
        }

        logger.info("Expiry check completed.");
    }

    public void checkStockForReorder(Map<Long, Map<Long, List<Product>>> stock) {
        logger.info("Checking all inventories in the depot for reorder and low stock alerts");

        stock.forEach((inventoryId, productSchemaMap) -> {
            Inventory inventory = inventoryService.getInventoryById(inventoryId);

            Map<ProductSchema, List<Product>> lowStockProducts = new HashMap<>();
            List<InternalReorder> reorderProducts = new ArrayList<>();

            productSchemaMap.forEach((productSchemaId, products) -> {
                if (LOW_STOCK_ALERT_ENABLED && inventoryService.lowOnStock(inventory, productSchemaId)) {
                    List<Product> temp = new ArrayList<>();
                    products.stream()
                            .filter(product -> product.getStatus() != ProductStatus.REMOVED)
                            .forEach(temp::add);
                    lowStockProducts.put(productSchemaService.getProductSchemaById(productSchemaId), temp);
                }

                if (AUTO_REORDER_ENABLED && inventoryService.needsReorderForStock(inventory, productSchemaId)) {
                    reorderProducts.add(new InternalReorder(productSchemaService.getProductSchemaById(productSchemaId), products.get(0).getDescription(), products.get(0).getExpiresAt()));
                }
            });

            if (!lowStockProducts.isEmpty()) {
                eventPublisher.publishEvent(new LowStockAlertEvent(this, inventory, lowStockProducts));
            }

            if (!reorderProducts.isEmpty()) {
                eventPublisher.publishEvent(new ReorderAlertEvent(this, inventory, reorderProducts));
            }
        });
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
}
