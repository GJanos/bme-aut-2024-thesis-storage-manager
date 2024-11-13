package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.product.InvalidProductExpiryException;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.supplier.NonGreaterThanZeroProductStockAddException;
import com.bme.vik.aut.thesis.depot.exception.user.UserSupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.ProductStockResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.RemoveProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {

    @Value("${custom.inventory.max-inventory-space}")
    private int MAX_AVAILABLE_SPACE;

    @Value("${custom.inventory.auto-reorder}")
    private boolean AUTO_REORDER_ENABLED;

    @Value("${custom.inventory.low-stock-alert}")
    private boolean LOW_STOCK_ALERT_ENABLED;

// TODO do this later use a timeservice class
//    @Value("${custom.inventory.expiry-alert}")
//    private boolean EXPIRY_ALERT_ENABLED;

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final ProductSchemaService productSchemaService;
    private final SupplierRepository supplierRepository;

    // { K: InventoryID, V: { K: ProductSchemaID, V: List<Product> } }
    private Map<Long, Map<Long, List<Product>>> stock = new HashMap<>();

    @Transactional
    public void initializeStockForAllInventories() {
        supplierRepository.findAll().forEach(supplier -> {
            Inventory inventory = supplier.getInventory();
            Long inventoryId = inventory.getId();
            List<Long> productIds = inventory.getProductIds();

            if (productIds != null && !productIds.isEmpty()) {
                Map<Long, List<Product>> inventoryStock = new HashMap<>();

                for (Long productId : productIds) {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ProductNotFoundException("Product with ID " + productId + " not found"));

                    inventoryStock.computeIfAbsent(product.getSchema().getId(), k -> new ArrayList<>()).add(product);
                }

                // Store the stock for the inventory in the main stock map
                stock.put(inventoryId, inventoryStock);
                System.out.println("Initialized stock for inventory with ID: " + inventoryId);
            }
        });
    }

    public Inventory createInventory(CreateSupplierRequest request) {
        logger.info("Creating new inventory for supplier with initial max space: {}", MAX_AVAILABLE_SPACE);

        Inventory inventory = Inventory.builder()
                .usedSpace(0)
                .maxAvailableSpace(MAX_AVAILABLE_SPACE)
                .lowStockAlertThreshold(request.getLowStockAlertThreshold())
                .expiryAlertThreshold(request.getExpiryAlertThreshold())
                .reorderThreshold(request.getReorderThreshold())
                .reorderQuantity(request.getReorderQuantity())
                .build();

        logger.info("Inventory created successfully");
        return inventory;
    }

    public Inventory updateInventory(Inventory inventory, CreateSupplierRequest request) {
        logger.info("Updating inventory with ID: {}", inventory.getId());

        inventory.setLowStockAlertThreshold(request.getLowStockAlertThreshold());
        inventory.setExpiryAlertThreshold(request.getExpiryAlertThreshold());
        inventory.setReorderThreshold(request.getReorderThreshold());
        inventory.setReorderQuantity(request.getReorderQuantity());
        logger.info("Inventory with ID {} updated successfully", inventory.getId());

        return inventory;
    }

    public Inventory getInventoryBySupplierId(Long supplierId) {
        logger.info("Fetching inventory by supplier ID: {}", supplierId);
        return inventoryRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory for supplier with ID " + supplierId + " not found"));
    }

    @Transactional
    public ProductStockResponse addStock(MyUser user, CreateProductStockRequest request) {
        // validate request
        validateSupplierExists(user);
        Long supplierId = user.getSupplier().getId();

        Inventory inventory = getInventoryBySupplierId(supplierId);
        Long inventoryId = inventory.getId();

        validateExpiryDate(request.getExpiresAt(), inventory);

        ProductSchema productSchema = productSchemaService.getProductSchemaById(request.getProductSchemaId());
        Long productSchemaId = productSchema.getId();
        validateProductSchema(inventoryId, productSchemaId);

        // validation complete
        int quantity = validatePositiveQuantity(request.getQuantity());
        String productName = productSchema.getName();

        logger.info("User '{}' with supplier ID: {} is adding: {} stock of: {}", user.getUsername(), supplierId, quantity, productName);

        // handle inventory full
        int fullSpaceNeeded = quantity * productSchema.getStorageSpaceNeeded();
        if (!inventory.hasAvailableSpace(fullSpaceNeeded)) {
            // TODO: emit inventory full
            int availableSpace = inventory.getMaxAvailableSpace() - inventory.getUsedSpace();
            String errorMsg = "Not enough space in inventory for supplier ID: " + supplierId + ". Available space: " + availableSpace + ", requested space: " + fullSpaceNeeded;
            logger.error(errorMsg);
            throw new InventoryFullException(errorMsg);
        }

        // create products and save them
        List<Product> productsToAdd = createProductsToAdd(request, quantity, productSchema);
        productRepository.saveAll(productsToAdd);

        // add products to inventory and save it
        inventory.addStock(productsToAdd);
        inventoryRepository.save(inventory);

        logger.info("Stock successfully added to inventory of supplier with ID: {}. Stock-add size: {}", supplierId, quantity);

        return ProductStockResponse.builder()
                .productSchemaId(productSchemaId)
                .quantity(quantity)
                .response("Stock added successfully")
                .build();
    }

    @Transactional
    public ProductStockResponse removeStock(MyUser user, RemoveProductStockRequest request) {
        // validate request
        String username = user.getUsername();
        validateSupplierExists(user);

        Long supplierId = user.getSupplier().getId();

        Inventory inventory = getInventoryBySupplierId(supplierId);
        Long inventoryId = inventory.getId();

        ProductSchema productSchema = productSchemaService.getProductSchemaById(request.getProductSchemaId());
        Long productSchemaId = productSchema.getId();
        validateProductSchema(inventoryId, productSchemaId);

        // validation complete
        int quantity = validatePositiveQuantity(request.getQuantity());
        String productName = productSchema.getName();

        logger.info("User '{}' with supplier ID: {} is removing: {} stock of: {}", username, supplierId, quantity, productName);

        if (!hasAvailableStock(inventoryId, productSchemaId, quantity)) {
            // TODO: emit inventory out of stock
            String errorMsg = "Too few stock of: " + productName + " in inventory for supplier ID: " + supplierId + ". Available stock: " + getCurrentStock(inventoryId, productSchemaId) + ", requested: " + quantity;
            logger.error(errorMsg);
            throw new InventoryOutOfStockException(errorMsg);
        }

        List<Product> productToRemove = createProductsToRemoveForNearExpiryStock(inventoryId, productSchemaId, quantity);
        productRepository.deleteAll(productToRemove);

        inventory.removeStock(productToRemove);
        inventoryRepository.save(inventory);

        logger.info("Stock successfully removed from inventory of supplier with ID: {} Stock-remove size: {}", supplierId, quantity);

        if (AUTO_REORDER_ENABLED && needsReorderForStock(inventory, productSchemaId)) {
            logger.info("Reordering {} stock of: {} for supplier with ID: {}", inventory.getReorderQuantity(), productName, supplierId);
            // TODO: emit reorder event which actually replenishes the stock
        }

        if (LOW_STOCK_ALERT_ENABLED && lowOnStock(inventory, productSchemaId)) {
            logger.info("Low stock alert for: {} for supplier with ID: {}", productName, supplierId);
            // TODO: emit low stock alert event which does something
        }

        return ProductStockResponse.builder()
                .productSchemaId(productSchemaId)
                .quantity(quantity)
                .response("Stock removed successfully")
                .build();
    }

    private void validateSupplierExists(MyUser user) {
        if (user.getSupplier() == null) {
            logger.error("User '{}' does not have a supplier", user.getUsername());
            throw new UserSupplierNotFoundException("User '" + user.getUsername() + "' does not have a supplier");
        }
    }

    private int validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            logger.error("Invalid stock quantity: {}. Quantity must be greater than zero.", quantity);
            throw new NonGreaterThanZeroProductStockAddException("Requested quantity: " + quantity + ", it must be greater than zero.");
        }
        return quantity;
    }

    private void validateExpiryDate(LocalDateTime expiresAt, Inventory inventory) {
        if (expiresAt == null || expiresAt.isBefore(inventory.getCreatedAt())) {
            logger.error("Invalid expiry date: {}. Expiry date must be provided, and be after inventory creation date.", expiresAt);
            throw new InvalidProductExpiryException("Invalid expiry date: " + expiresAt + ". Expiry date must be after inventory creation date.");
        }
    }

    private void validateProductSchema(Long inventoryId, Long productSchemaId) {
        if (!stock.containsKey(inventoryId)) {
            throw new InventoryNotFoundException("No inventory found with ID " + inventoryId);
        }
        if (!stock.get(inventoryId).containsKey(productSchemaId)) {
            throw new ProductNotFoundException("No products of schema " + productSchemaId + " found in inventory with ID " + inventoryId);
        }
    }

    private List<Product> createProductsToAdd(CreateProductStockRequest request, int quantity, ProductSchema productSchema) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            products.add(Product.builder()
                    .schema(productSchema)
                    .description(request.getDescription())
                    .expiresAt(request.getExpiresAt())
                    .build());
        }
        return products;
    }

    private int getCurrentStock(Long inventoryId, Long productSchemaId) {
        return stock.get(inventoryId).get(productSchemaId).size();
    }

    private boolean hasAvailableStock(Long inventoryId, Long productSchemaId, int quantity) {
        return getCurrentStock(inventoryId, productSchemaId) >= quantity;
    }

    private List<Product> createProductsToRemoveForNearExpiryStock(Long inventoryId, Long productSchemaId, int quantity) {
        Map<Long, List<Product>> inventoryStock = stock.get(inventoryId);
        List<Product> inventoryProducts = inventoryStock.get(productSchemaId);

        // Sort products by expiry date in ascending order (earliest expiry first)
        inventoryProducts.sort(Comparator.comparing(Product::getExpiresAt));

        List<Product> productsToRemove = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Product product = inventoryProducts.remove(0);
            productsToRemove.add(product);
        }

        // TODO or maybe dont remove it and let size be one, but it would require other code changes as well
        if (inventoryProducts.isEmpty()) {
            inventoryStock.remove(productSchemaId);
        }
        return productsToRemove;
    }

    private boolean needsReorderForStock(Inventory inventory, Long productSchemaId) {
        return getCurrentStock(inventory.getId(), productSchemaId) <= inventory.getReorderThreshold();
    }

    private boolean lowOnStock(Inventory inventory, Long productSchemaId) {
        return getCurrentStock(inventory.getId(), productSchemaId) <= inventory.getLowStockAlertThreshold();
    }
}
