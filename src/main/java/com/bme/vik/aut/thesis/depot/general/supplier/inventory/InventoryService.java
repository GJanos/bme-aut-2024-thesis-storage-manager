package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.exception.inventory.DepotFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.product.InvalidProductExpiryException;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.supplier.NonGreaterThanZeroQuantityException;
import com.bme.vik.aut.thesis.depot.exception.user.UserSupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.AlertService;
import com.bme.vik.aut.thesis.depot.general.report.ReportService;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryState;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.ProductStockResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.RemoveProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.min;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Value("${custom.inventory.max-depot-space}")
    private int AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY;

    @Value("${custom.inventory.max-inventory-space}")
    private int MAX_AVAILABLE_INVENTORY_SPACE;

    @Value("${custom.inventory.should-check-expiration}")
    private boolean SHOULD_CHECK_EXPIRATION;

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final ProductSchemaService productSchemaService;
    private final SupplierRepository supplierRepository;
    private final AlertService alertService;
    private final ReportService reportService;

    // { K: InventoryID, V: { K: ProductSchemaID, V: List<Product> } }
    @Getter
    private final Map<Long, Map<Long, List<Product>>> stock = new HashMap<>();

    // :::::::::::::::::::::::::::::::::::::::::::::: //
    // @@@@@@@@@@@@@@@ PUBLIC METHODS @@@@@@@@@@@@@@@ //
    // :::::::::::::::::::::::::::::::::::::::::::::: //

    @PostConstruct
    public void init() {
        alertService.setInventoryService(this);
    }

    public void clearStock() {
        stock.clear();
    }

    @Transactional
    public void initializeStockForAllInventories() {
        supplierRepository.findAll().forEach(supplier -> {
            Inventory inventory = supplier.getInventory();
            Long inventoryId = inventory.getId();
            List<Long> productIds = inventory.getProductIds();
            AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY -= inventory.getMaxAvailableSpace();

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
        logger.info("Creating new inventory for supplier with initial max space: {}", MAX_AVAILABLE_INVENTORY_SPACE);

        if (AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY - MAX_AVAILABLE_INVENTORY_SPACE <= 0) {
            String errorMsg = "Not enough space in depot for new inventory. Available space: " + AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY + ", requested space: " + MAX_AVAILABLE_INVENTORY_SPACE;
            logger.error(errorMsg);
            throw new DepotFullException(errorMsg);
        }

        AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY -= MAX_AVAILABLE_INVENTORY_SPACE;

        Inventory inventory = Inventory.builder()
                .usedSpace(0)
                .maxAvailableSpace(MAX_AVAILABLE_INVENTORY_SPACE)
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

    public Inventory getInventoryById(Long inventoryId) {
        logger.info("Fetching inventory by ID: {}", inventoryId);
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory with ID " + inventoryId + " not found"));
    }

    public InventoryState getInventoryStateBySupplierId(Long supplierId) {
        logger.info("Fetching inventory state by supplier ID: {}", supplierId);

        Inventory inventory = inventoryRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory for supplier with ID " + supplierId + " not found"));

        return reportService.mapToInventoryState(inventory);
    }

    public Inventory getInventoryBySupplierId(Long supplierId) {
        logger.info("Fetching inventory by supplier ID: {}", supplierId);
        return inventoryRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory for supplier with ID " + supplierId + " not found"));
    }

    public Inventory getByProductId(Long productId) {
        logger.info("Fetching inventory by product ID: {}", productId);
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory for product with ID " + productId + " not found"));
    }

    public List<Inventory> getInventoriesWithStockForSchema(Long productSchemaId) {
        logger.info("Fetching inventories with stock for product schema with ID: {}", productSchemaId);
        return inventoryRepository.findAllByProductSchemaId(productSchemaId);
    }

    @Transactional
    public ProductStockResponse addStock(MyUser user, CreateProductStockRequest request) {
        // validate request
        validateSupplierExists(user);
        Long supplierId = user.getSupplier().getId();

        Inventory inventory = getInventoryBySupplierId(supplierId);
        Long inventoryId = inventory.getId();

        if (SHOULD_CHECK_EXPIRATION) {
            validateExpiryDate(request.getExpiresAt(), inventory);
        }

        int quantity = validatePositiveQuantity(request.getQuantity());

        ProductSchema productSchema = productSchemaService.getProductSchemaById(request.getProductSchemaId());
        Long productSchemaId = productSchema.getId();
        String productName = productSchema.getName();

        logger.info("User '{}' with supplier ID: {} is adding: {} stock of: {}", user.getUsername(), supplierId, quantity, productName);

        // handle inventory full
        int fullSpaceNeeded = quantity * productSchema.getStorageSpaceNeeded();
        if (!inventory.hasAvailableSpace(fullSpaceNeeded)) {
            int availableSpace = inventory.getMaxAvailableSpace() - inventory.getUsedSpace();
            String errorMsg = "Not enough space in inventory for supplier ID: " + supplierId + ". Available space: " + availableSpace + ", requested space: " + fullSpaceNeeded;
            logger.error(errorMsg);
            throw new InventoryFullException(errorMsg);
        }

        // create products and save them
        List<Product> productsToAdd = createProductsToAdd(request, quantity, productSchema, inventory);
        productRepository.saveAll(productsToAdd);

        // add products to stock
        addToInMemoryStock(inventoryId, productSchemaId, productsToAdd);

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

        int quantity = validatePositiveQuantity(request.getQuantity());

        ProductSchema productSchema = productSchemaService.getProductSchemaById(request.getProductSchemaId());
        Long productSchemaId = productSchema.getId();
        validateProductSchema(inventoryId, productSchemaId);
        String productName = productSchema.getName();

        logger.info("User '{}' with supplier ID: {} is removing: {} stock of: {}", username, supplierId, quantity, productName);

        if (!hasAvailableStock(inventoryId, productSchemaId, quantity)) {
            String errorMsg = "Too few stock of: " + productName + " in inventory for supplier ID: " + supplierId + ". Available stock: " + getCurrentStock(inventoryId, productSchemaId) + ", requested: " + quantity;
            logger.error(errorMsg);
            throw new InventoryOutOfStockException(errorMsg);
        }

        // create products to remove and remove them
        List<Product> productToRemove = createProductsToRemoveForNearExpiryStock(inventoryId, productSchemaId, quantity);
        productRepository.deleteAll(productToRemove);

        // remove products from inventory and save it
        inventory.removeStock(productToRemove);
        inventoryRepository.save(inventory);

        logger.info("Stock successfully removed from inventory of supplier with ID: {} Stock-remove size: {}", supplierId, quantity);

        // supplier removed its own stock, so no need to check for reorder or low stock alert

        return ProductStockResponse.builder()
                .productSchemaId(productSchemaId)
                .quantity(quantity)
                .response("Stock removed successfully")
                .build();
    }

    @Transactional
    public List<Product> getAllProductsInInventoryForUser(MyUser user) {
        // TODO might just inventoryRepository.findAllProducts()
        validateSupplierExists(user);
        Long supplierId = user.getSupplier().getId();
        Inventory inventory = getInventoryBySupplierId(supplierId);
        Long inventoryId = inventory.getId();

        if (!stock.containsKey(inventoryId)) {
            logger.warn("No products found in inventory with ID {}", inventoryId);
            return new ArrayList<>();
        }

        List<Product> allProducts = new ArrayList<>();
        stock.get(inventoryId).values().forEach(allProducts::addAll);

        logger.info("Fetched all products in inventory for supplier with ID: {}", supplierId);
        return allProducts;
    }

    @Transactional
    public void reserveOneProduct(Inventory inventory, Product product) {
        Long inventoryId = inventory.getId();
        Long schemaId = product.getSchema().getId();
        Long productId = product.getId();

        logger.info("Reserving product with ID: {} in inventory with ID: {}", productId, inventoryId);

        changeProductStatus(inventoryId, schemaId, productId, ProductStatus.RESERVED);
    }

    @Transactional
    public List<Product> reserveProdByProdSupplName(Inventory inventory, ProductSchema schema, int quantity) {
        Long inventoryId = inventory.getId();
        Long schemaId = schema.getId();

        validateProductSchema(inventoryId, schemaId);

        return reserveProducts(inventoryId, schemaId, quantity);
    }

    @Transactional
    public List<Product> reserveProdByProdName(Inventory inventory, ProductSchema schema, int quantity) {
        Long inventoryId = inventory.getId();
        Long schemaId = schema.getId();

        validateProductSchema(inventoryId, schemaId);

        int neededStockSize = min(quantity, getCurrentStock(inventoryId, schemaId));

        return reserveProducts(inventoryId, schemaId, neededStockSize);
    }

    @Transactional
    public void freeProducts(List<Product> products) {
        products.forEach(product -> {
            Long inventoryId = getInventoryBySupplierId(product.getSupplierId()).getId();
            Long schemaId = product.getSchema().getId();
            Long productId = product.getId();

            changeProductStatus(inventoryId, schemaId, productId, ProductStatus.FREE);
        });

        logger.info("Freed {} products", products.size());
    }

    @Transactional
    public void removeCompletedOrderProducts(List<Product> orderProducts) {
        orderProducts.forEach(orderProduct -> {
            Long inventoryId = getInventoryBySupplierId(orderProduct.getSupplierId()).getId();
            Long schemaId = orderProduct.getSchema().getId();
            Long productId = orderProduct.getId();

            changeProductStatus(inventoryId, schemaId, productId, ProductStatus.REMOVED);
        });

        logger.info("Marked {} products as removed from inventory based on completed order.", orderProducts.size());

        alertService.checkStockForReorder(stock);
    }

    @Transactional
    public void changeProductExpirationStatus(Long inventoryId, Long schemaId, Long productId, ExpiryStatus expiryStatus) {
        List<Product> products = stock.get(inventoryId).get(schemaId);

        products.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .ifPresent(p -> {
                    p.setExpiryStatus(expiryStatus);
                    productRepository.save(p);
                });
    }

    @Transactional
    public void changeProductStatus(Long inventoryId, Long schemaId, Long productId, ProductStatus status) {
        List<Product> products = stock.get(inventoryId).get(schemaId);

        products.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus(status);
                    productRepository.save(p);
                });
    }

    public int getCurrentStockBySchemaId(Long productSchemaId) {
        return stock.values().stream()
                .filter(schemaMap -> schemaMap.containsKey(productSchemaId))
                .flatMap(schemaMap -> schemaMap.get(productSchemaId).stream())
                .mapToInt(product -> product.getStatus() == ProductStatus.FREE ? 1 : 0)
                .sum();
    }

    public boolean needsReorderForStock(Inventory inventory, Long productSchemaId) {
        return getCurrentStock(inventory.getId(), productSchemaId) <= inventory.getReorderThreshold();
    }

    public boolean lowOnStock(Inventory inventory, Long productSchemaId) {
        return getCurrentStock(inventory.getId(), productSchemaId) <= inventory.getLowStockAlertThreshold();
    }

    public int validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            logger.error("Invalid stock quantity: {}. Quantity must be greater than zero.", quantity);
            throw new NonGreaterThanZeroQuantityException("Requested quantity: " + quantity + ", it must be greater than zero.");
        }
        return quantity;
    }

    public int getCurrentStock(Long inventoryId, Long productSchemaId) {
        if (!stock.containsKey(inventoryId) || !stock.get(inventoryId).containsKey(productSchemaId)) {
            return 0;
        }

        // Filter out reserved products and count only non-reserved products
        return (int) stock.get(inventoryId).get(productSchemaId).stream()
                .filter(product -> product.getStatus() == ProductStatus.FREE)
                .count();
    }

    public Product getSoonestExpiryProduct(Inventory inventory, ProductSchema schema) {
        // Fetch all products of the specified schema in the inventory
        List<Product> products = stock.get(inventory.getId()).get(schema.getId());

        // Find the free product with the closest expiry date
        return products.stream()
                .filter(product -> product.getStatus() == ProductStatus.FREE)
                .min(Comparator.comparing(Product::getExpiresAt))
                .orElseThrow(() -> new ProductNotFoundException("No available products with schema ID " + schema.getId() + " in inventory ID " + inventory.getId()));
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::: //
    // @@@@@@@@@@@@@@@ PRIVATE METHODS @@@@@@@@@@@@@@@ //
    // ::::::::::::::::::::::::::::::::::::::::::::::: //

    private List<Product> reserveProducts(Long inventoryId, Long schemaId, int quantity) {
        List<Product> products = stock.get(inventoryId).get(schemaId);

        List<Product> productsToReserve = selectCTEFreeProducts(products, quantity);

        // Reserve the selected products
        productsToReserve.forEach(product -> {
            product.setStatus(ProductStatus.RESERVED);
            productRepository.save(product);
        });

        return productsToReserve;
    }

    private void validateSupplierExists(MyUser user) {
        if (user.getSupplier() == null) {
            logger.error("User '{}' does not have a supplier", user.getUsername());
            throw new UserSupplierNotFoundException("User '" + user.getUsername() + "' does not have a supplier");
        }
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

    private List<Product> createProductsToAdd(CreateProductStockRequest request, int quantity, ProductSchema productSchema, Inventory inventory) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            products.add(Product.builder()
                    .schema(productSchema)
                    .supplierId(inventory.getSupplier().getId())
                    .description(request.getDescription())
                    .status(ProductStatus.FREE)
                    .expiryStatus(alertService.determineExpiryStatus(request.getExpiresAt(), inventory.getExpiryAlertThreshold()))
                    .expiresAt(request.getExpiresAt())
                    .build());
        }
        return products;
    }

    private void addToInMemoryStock(Long inventoryId, Long productSchemaId, List<Product> productsToAdd) {
        if (!stock.containsKey(inventoryId)) {
            stock.put(inventoryId, new HashMap<>());
        }

        if (!stock.get(inventoryId).containsKey(productSchemaId)) {
            stock.get(inventoryId).put(productSchemaId, new ArrayList<>());
        }

        stock.get(inventoryId).get(productSchemaId).addAll(productsToAdd);
    }

    private boolean hasAvailableStock(Long inventoryId, Long productSchemaId, int quantity) {
        return getCurrentStock(inventoryId, productSchemaId) >= quantity;
    }

    private List<Product> selectCTEFreeProducts(List<Product> products, int quantity) {
        return products.stream()
                .filter(product -> product.getStatus() == ProductStatus.FREE)
                .sorted(Comparator.comparing(Product::getExpiresAt))
                .limit(quantity)
                .collect(Collectors.toList());
    }

    private List<Product> createProductsToRemoveForNearExpiryStock(Long inventoryId, Long productSchemaId, int quantity) {
        Map<Long, List<Product>> inventoryStock = stock.get(inventoryId);
        List<Product> inventoryProducts = inventoryStock.get(productSchemaId);

        List<Product> productsToRemove = selectCTEFreeProducts(inventoryProducts, quantity);

        inventoryProducts.removeAll(productsToRemove);

        if (inventoryProducts.isEmpty()) {
            inventoryStock.remove(productSchemaId);
        }
        return productsToRemove;
    }
}
