package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TimeService timeService;

    @Mock
    private ProductService productService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ProductSchemaService productSchemaService;

    @InjectMocks
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertService, "AUTO_REORDER_ENABLED", true);
        ReflectionTestUtils.setField(alertService, "LOW_STOCK_ALERT_ENABLED", true);
        ReflectionTestUtils.setField(alertService, "EXPIRY_ALERT_ENABLED", true);

        alertService.setInventoryService(inventoryService);
    }

    @Test
    void shouldCorrectlyDetermineExpiryStatus() {
        //***** <-- given: Mock current time --> *****//
        LocalDateTime now = LocalDateTime.now();
        when(timeService.getCurrentTime()).thenReturn(now);

        int expiryAlertThreshold = 7; // Days before expiry to trigger "SOONTOEXPIRE"

        //***** <-- given: Test case inputs --> *****//
        LocalDateTime longExpiredDate = now.minusDays(31);
        LocalDateTime expiredDate = now.minusDays(1);
        LocalDateTime soonToExpireDate = now.plusDays(6);
        LocalDateTime notExpiredDate = now.plusDays(10);

        //***** <-- when & then: Verify each case --> *****//
        assertEquals(ExpiryStatus.LONGEXPIRED, alertService.determineExpiryStatus(longExpiredDate, expiryAlertThreshold));
        assertEquals(ExpiryStatus.EXPIRED, alertService.determineExpiryStatus(expiredDate, expiryAlertThreshold));
        assertEquals(ExpiryStatus.SOONTOEXPIRE, alertService.determineExpiryStatus(soonToExpireDate, expiryAlertThreshold));
        assertEquals(ExpiryStatus.NOTEXPIRED, alertService.determineExpiryStatus(notExpiredDate, expiryAlertThreshold));
    }

    @Test
    void shouldSkipExpiredProductCheckWhenAlertDisabled() {
        //***** <-- given: Expiry alert disabled --> *****//
        ReflectionTestUtils.setField(alertService, "EXPIRY_ALERT_ENABLED", false);

        //***** <-- when: Expiry check is triggered --> *****//
        alertService.checkForExpiredProducts();

        //***** <-- then: Ensure no interactions with dependencies --> *****//
        verify(inventoryRepository, never()).findAll();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldNotEmitExpiredProductEventWhenNoInventoriesPresent() {
        //***** <-- given: No inventories present in the repository --> *****//
        when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());

        //***** <-- when: Expiry check is triggered --> *****//
        alertService.checkForExpiredProducts();

        //***** <-- then: Verify no events are emitted --> *****//
        verify(eventPublisher, never()).publishEvent(any(ProductExpiredAlertEvent.class));
    }

    @Test
    void shouldEmitExpiredProductEvent() {
        //***** <-- given: Create inventories and products --> *****//
        int expiryAlertThreshold = 7;

        Inventory inventory1 = Inventory.builder()
                .id(1L)
                .expiryAlertThreshold(expiryAlertThreshold)
                .productIds(List.of(1L, 2L))
                .build();

        Inventory inventory2 = Inventory.builder()
                .id(2L)
                .expiryAlertThreshold(expiryAlertThreshold)
                .productIds(List.of(3L, 4L))
                .build();

        Product longExpiredProduct = Product.builder()
                .id(1L)
                .expiresAt(LocalDateTime.now().minusDays(31))
                .expiryStatus(ExpiryStatus.NOTEXPIRED)
                .schema(ProductSchema.builder().id(1L).build())
                .build();

        Product expiredProduct = Product.builder()
                .id(2L)
                .expiresAt(LocalDateTime.now().minusDays(10))
                .expiryStatus(ExpiryStatus.NOTEXPIRED)
                .schema(ProductSchema.builder().id(2L).build())
                .build();

        Product soonToExpireProduct = Product.builder()
                .id(3L)
                .expiresAt(LocalDateTime.now().plusDays(expiryAlertThreshold - 1))
                .expiryStatus(ExpiryStatus.NOTEXPIRED)
                .schema(ProductSchema.builder().id(3L).build())
                .build();

        Product notExpiredProduct = Product.builder()
                .id(4L)
                .expiresAt(LocalDateTime.now().plusDays(expiryAlertThreshold + 1))
                .expiryStatus(ExpiryStatus.NOTEXPIRED)
                .schema(ProductSchema.builder().id(4L).build())
                .build();

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory1, inventory2));
        when(productService.getProductById(1L)).thenReturn(longExpiredProduct);
        when(productService.getProductById(2L)).thenReturn(expiredProduct);
        when(productService.getProductById(3L)).thenReturn(soonToExpireProduct);
        when(productService.getProductById(4L)).thenReturn(notExpiredProduct);
        doNothing().when(inventoryService).changeProductExpirationStatus(any(), any(), any(), any());
        when(timeService.getCurrentTime()).thenReturn(LocalDateTime.now());

        //***** <-- when: Expiry check is triggered --> *****//
        alertService.checkForExpiredProducts();

        //***** <-- then: Verify correct events are emitted --> *****//

        // Verify for inventory1
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            if (event instanceof ProductExpiredAlertEvent expiredEvent) {
                List<Product> products = expiredEvent.getProducts();
                return expiredEvent.getInventory().getId() == 1L
                        && products.size() == 2
                        && products.stream().anyMatch(p -> p.getId() == 1L && p.getExpiryStatus() == ExpiryStatus.LONGEXPIRED)
                        && products.stream().anyMatch(p -> p.getId() == 2L && p.getExpiryStatus() == ExpiryStatus.EXPIRED);
            }
            return false;
        }));

        // Verify for inventory2
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            if (event instanceof ProductExpiredAlertEvent expiredEvent) {
                List<Product> products = expiredEvent.getProducts();
                return expiredEvent.getInventory().getId() == 2L
                        && products.size() == 1
                        && products.stream().anyMatch(p -> p.getId() == 3L && p.getExpiryStatus() == ExpiryStatus.SOONTOEXPIRE);
            }
            return false;
        }));
    }

    @Test
    void shouldNotEmitEventsWhenStockIsEmptyEvenWhileEnabled() {
        //***** <-- given: Empty stock map --> *****//
        Map<Long, Map<Long, List<Product>>> stock = Collections.emptyMap();

        //***** <-- when: Stock check is triggered --> *****//
        alertService.checkStockForReorder(stock);

        //***** <-- then: Verify no events are emitted --> *****//
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldNotEmitEventsForProductsNotRequiringReorderOrLowStockEvenWhileEnabled() {
        //***** <-- given: Stock with products not requiring reorder or low stock --> *****//
        Long inventoryId = 1L;
        Long productSchemaId = 1L;
        Long supplierId = 100L;

        int lowStockAlertThreshold = 3;
        int reorderThreshold = 5;
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder().id(supplierId).build())
                .lowStockAlertThreshold(lowStockAlertThreshold)
                .reorderThreshold(reorderThreshold)
                .build();

        Product product = Product.builder()
                .id(1L)
                .schema(ProductSchema.builder().id(productSchemaId).name("Test Product").build())
                .status(ProductStatus.FREE)
                .build();

        Map<Long, Map<Long, List<Product>>> stock = Map.of(
                inventoryId, Map.of(productSchemaId, Collections.nCopies(reorderThreshold + 1, product))
        ); // stock size does not matter

        when(inventoryService.getInventoryById(inventoryId)).thenReturn(inventory);
        when(inventoryService.lowOnStock(inventory, productSchemaId)).thenReturn(false);
        when(inventoryService.needsReorderForStock(inventory, productSchemaId)).thenReturn(false);

        //***** <-- when: Stock check is triggered --> *****//
        alertService.checkStockForReorder(stock);

        //***** <-- then: Verify no events are emitted --> *****//
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldNotEmitEventsForProductsNotRequiringReorderOrLowStockWhenNotEnabled() {
        //***** <-- given: Stock with products not requiring reorder or low stock --> *****//
        ReflectionTestUtils.setField(alertService, "AUTO_REORDER_ENABLED", false);
        ReflectionTestUtils.setField(alertService, "LOW_STOCK_ALERT_ENABLED", false);
        Long inventoryId = 1L;
        Long productSchemaId = 1L;
        Long supplierId = 100L;

        int lowStockAlertThreshold = 3;
        int reorderThreshold = 5;
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder().id(supplierId).build())
                .lowStockAlertThreshold(lowStockAlertThreshold)
                .reorderThreshold(reorderThreshold)
                .build();

        Product product = Product.builder()
                .id(1L)
                .schema(ProductSchema.builder().id(productSchemaId).name("Test Product").build())
                .status(ProductStatus.FREE)
                .build();

        Map<Long, Map<Long, List<Product>>> stock = Map.of(
                inventoryId, Map.of(productSchemaId, Collections.nCopies(reorderThreshold + 1, product))
        ); // stock size does not matter

        when(inventoryService.getInventoryById(inventoryId)).thenReturn(inventory);

        //***** <-- when: Stock check is triggered --> *****//
        alertService.checkStockForReorder(stock);

        //***** <-- then: Verify no events are emitted --> *****//
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldEmitLowStockAlertEvent() {
        //***** <-- given: Stock with a product requiring low stock alert --> *****//
        ReflectionTestUtils.setField(alertService, "AUTO_REORDER_ENABLED", false);

        Long inventoryId = 1L;
        Long productSchemaId = 1L;
        String productName = "Test Product 1";
        Long productSchemaId3 = 3L;
        String productName3 = "Test Product 3";
        Long supplierId = 100L;

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder().id(supplierId).build())
                .build();

        ProductSchema productSchema1 = ProductSchema.builder().id(productSchemaId).name(productName).build();
        ProductSchema productSchema3 = ProductSchema.builder().id(productSchemaId3).name(productName3).build();

        Product product1 = Product.builder()
                .id(1L)
                .schema(productSchema1)
                .status(ProductStatus.FREE)
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .schema(productSchema1)
                .status(ProductStatus.FREE)
                .build();

        Product product3 = Product.builder()
                .id(3L)
                .schema(productSchema3)
                .status(ProductStatus.FREE)
                .build();

        Map<Long, Map<Long, List<Product>>> stock = Map.of(
                inventoryId, Map.of(
                        productSchemaId, List.of(product1, product2),
                        productSchemaId3, List.of(product3)
                )
        );

        when(inventoryService.getInventoryById(inventoryId)).thenReturn(inventory);
        when(productSchemaService.getProductSchemaById(productSchemaId)).thenReturn(productSchema1);
        when(productSchemaService.getProductSchemaById(productSchemaId3)).thenReturn(productSchema3);
        when(inventoryService.lowOnStock(inventory, productSchemaId)).thenReturn(true);
        when(inventoryService.lowOnStock(inventory, productSchemaId3)).thenReturn(true);

        //***** <-- when: Stock check is triggered --> *****//
        alertService.checkStockForReorder(stock);

        //***** <-- then: Verify low stock alert event is emitted --> *****//
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            if (event instanceof LowStockAlertEvent lowStockEvent) {
                Map<ProductSchema, List<Product>> stockMap = lowStockEvent.getStock();

                // Verify event is for the correct inventory
                if (!lowStockEvent.getInventory().getId().equals(inventoryId)) {
                    return false;
                }

                // Verify stock contains the correct mappings
                return stockMap.size() == 2 &&
                        stockMap.containsKey(productSchema1) &&
                        stockMap.get(productSchema1).size() == 2 &&
                        stockMap.get(productSchema1).containsAll(List.of(product1, product2)) &&
                        stockMap.containsKey(productSchema3) &&
                        stockMap.get(productSchema3).size() == 1 &&
                        stockMap.get(productSchema3).contains(product3);
            }
            return false;
        }));
    }

    @Test
    void shouldEmitReorderAlertEvent() {
        //***** <-- given: Stock with a product requiring reorder alert --> *****//
        ReflectionTestUtils.setField(alertService, "LOW_STOCK_ALERT_ENABLED", false);

        Long inventoryId = 1L;
        Long productSchemaId = 1L;
        String productName = "Test Product 1";
        Long productSchemaId3 = 3L;
        String productName3 = "Test Product 3";
        Long supplierId = 100L;

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder().id(supplierId).build())
                .build();

        ProductSchema productSchema1 = ProductSchema.builder().id(productSchemaId).name(productName).build();
        ProductSchema productSchema3 = ProductSchema.builder().id(productSchemaId3).name(productName3).build();

        Product product1 = Product.builder()
                .id(1L)
                .schema(productSchema1)
                .status(ProductStatus.FREE)
                .description("Description for Product 1")
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();

        Product product3 = Product.builder()
                .id(3L)
                .schema(productSchema3)
                .status(ProductStatus.FREE)
                .description("Description for Product 3")
                .expiresAt(LocalDateTime.now().plusDays(15))
                .build();

        Map<Long, Map<Long, List<Product>>> stock = Map.of(
                inventoryId, Map.of(
                        productSchemaId, List.of(product1),
                        productSchemaId3, List.of(product3)
                )
        );

        when(inventoryService.getInventoryById(inventoryId)).thenReturn(inventory);
        when(productSchemaService.getProductSchemaById(productSchemaId)).thenReturn(productSchema1);
        when(productSchemaService.getProductSchemaById(productSchemaId3)).thenReturn(productSchema3);
        when(inventoryService.needsReorderForStock(inventory, productSchemaId)).thenReturn(true);
        when(inventoryService.needsReorderForStock(inventory, productSchemaId3)).thenReturn(true);

        //***** <-- when: Stock check is triggered --> *****//
        alertService.checkStockForReorder(stock);

        //***** <-- then: Verify reorder alert event is emitted --> *****//
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            if (event instanceof ReorderAlertEvent reorderAlertEvent) {
                List<InternalReorder> reorders = reorderAlertEvent.getReorders();

                // Verify event is for the correct inventory
                if (!reorderAlertEvent.getInventory().getId().equals(inventoryId)) {
                    return false;
                }

                // Verify reorders contain correct details
                return reorders.size() == 2 &&
                        reorders.stream().anyMatch(r ->
                                r.getProductSchema().getId().equals(productSchemaId) &&
                                        r.getProductSchema().getName().equals(productName) &&
                                        r.getProductDescription().equals("Description for Product 1") &&
                                        r.getExpiresAt().equals(product1.getExpiresAt())
                        ) &&
                        reorders.stream().anyMatch(r ->
                                r.getProductSchema().getId().equals(productSchemaId3) &&
                                        r.getProductSchema().getName().equals(productName3) &&
                                        r.getProductDescription().equals("Description for Product 3") &&
                                        r.getExpiresAt().equals(product3.getExpiresAt())
                        );
            }
            return false;
        }));
    }

    @Test
    void shouldEmitBothLowStockAndReorderAlertEvents() {
        //***** <-- given: Stock with products requiring both alerts --> *****//
        ReflectionTestUtils.setField(alertService, "AUTO_REORDER_ENABLED", true);
        ReflectionTestUtils.setField(alertService, "LOW_STOCK_ALERT_ENABLED", true);

        Long inventoryId = 1L;
        Long productSchemaId = 1L;
        String productName = "Test Product 1";
        Long productSchemaId3 = 3L;
        String productName3 = "Test Product 3";
        Long supplierId = 100L;

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder().id(supplierId).build())
                .build();

        ProductSchema productSchema1 = ProductSchema.builder().id(productSchemaId).name(productName).build();
        ProductSchema productSchema3 = ProductSchema.builder().id(productSchemaId3).name(productName3).build();

        Product product1 = Product.builder()
                .id(1L)
                .schema(productSchema1)
                .status(ProductStatus.FREE)
                .description("Description for Product 1")
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();

        Product product3 = Product.builder()
                .id(3L)
                .schema(productSchema3)
                .status(ProductStatus.FREE)
                .description("Description for Product 3")
                .expiresAt(LocalDateTime.now().plusDays(15))
                .build();

        Map<Long, Map<Long, List<Product>>> stock = Map.of(
                inventoryId, Map.of(
                        productSchemaId, List.of(product1),
                        productSchemaId3, List.of(product3)
                )
        );

        when(inventoryService.getInventoryById(inventoryId)).thenReturn(inventory);
        when(productSchemaService.getProductSchemaById(productSchemaId)).thenReturn(productSchema1);
        when(productSchemaService.getProductSchemaById(productSchemaId3)).thenReturn(productSchema3);
        when(inventoryService.lowOnStock(inventory, productSchemaId)).thenReturn(true);
        when(inventoryService.lowOnStock(inventory, productSchemaId3)).thenReturn(true);
        when(inventoryService.needsReorderForStock(inventory, productSchemaId)).thenReturn(true);
        when(inventoryService.needsReorderForStock(inventory, productSchemaId3)).thenReturn(true);

        //***** <-- when: Stock check is triggered --> *****//
        alertService.checkStockForReorder(stock);

        //***** <-- then: Verify low stock alert event is emitted --> *****//
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            if (event instanceof LowStockAlertEvent lowStockEvent) {
                Map<ProductSchema, List<Product>> stockMap = lowStockEvent.getStock();

                // Verify event is for the correct inventory
                if (!lowStockEvent.getInventory().getId().equals(inventoryId)) {
                    return false;
                }

                // Verify stock contains the correct mappings
                return stockMap.size() == 2 &&
                        stockMap.containsKey(productSchema1) &&
                        stockMap.get(productSchema1).size() == 1 &&
                        stockMap.get(productSchema1).contains(product1) &&
                        stockMap.containsKey(productSchema3) &&
                        stockMap.get(productSchema3).size() == 1 &&
                        stockMap.get(productSchema3).contains(product3);
            }
            return false;
        }));

        //***** <-- then: Verify reorder alert event is emitted --> *****//
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            if (event instanceof ReorderAlertEvent reorderAlertEvent) {
                List<InternalReorder> reorders = reorderAlertEvent.getReorders();

                // Verify event is for the correct inventory
                if (!reorderAlertEvent.getInventory().getId().equals(inventoryId)) {
                    return false;
                }

                // Verify reorders contain correct details
                return reorders.size() == 2 &&
                        reorders.stream().anyMatch(r ->
                                r.getProductSchema().getId().equals(productSchemaId) &&
                                        r.getProductSchema().getName().equals(productName) &&
                                        r.getProductDescription().equals("Description for Product 1") &&
                                        r.getExpiresAt().equals(product1.getExpiresAt())
                        ) &&
                        reorders.stream().anyMatch(r ->
                                r.getProductSchema().getId().equals(productSchemaId3) &&
                                        r.getProductSchema().getName().equals(productName3) &&
                                        r.getProductDescription().equals("Description for Product 3") &&
                                        r.getExpiresAt().equals(product3.getExpiresAt())
                        );
            }
            return false;
        }));
    }

}