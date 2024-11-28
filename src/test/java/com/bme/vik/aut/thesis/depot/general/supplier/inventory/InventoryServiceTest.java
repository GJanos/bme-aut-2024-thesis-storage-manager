package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.exception.inventory.DepotFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.product.InvalidProductExpiryException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
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
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private ReportService reportService;

    @Mock
    private ProductSchemaService productSchemaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryService, "AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY", 10000);
        ReflectionTestUtils.setField(inventoryService, "MAX_AVAILABLE_INVENTORY_SPACE", 1000);
        ReflectionTestUtils.setField(inventoryService, "SHOULD_CHECK_EXPIRATION", true);
        inventoryService.init();
    }

    @Test
    void shouldClearStockSuccessfully() {
        //***** <-- given: Initialize stock --> *****//
        Long inventoryId = 1L;
        Long productSchemaId = 101L;
        Long productId = 1001L;

        Product product = Product.builder()
                .id(productId)
                .schema(ProductSchema.builder().id(productSchemaId).build())
                .build();

        inventoryService.getStock().put(inventoryId, Map.of(productSchemaId, List.of(product)));

        //***** <-- when: Clear the stock --> *****//
        inventoryService.clearStock();

        //***** <-- then: Verify stock is cleared --> *****//
        assertTrue(inventoryService.getStock().isEmpty());
    }

    @Test
    void shouldInitializeStockForAllInventoriesSuccessfully() {
        //***** <-- given: Suppliers, inventories, and products setup --> *****//
        Long supplierId = 1L;
        Long inventoryId = 10L;
        Long productId1 = 100L;
        Long productId2 = 101L;
        Long productSchemaId1 = 200L;
        Long productSchemaId2 = 201L;

        ProductSchema productSchema1 = ProductSchema.builder().id(productSchemaId1).build();
        ProductSchema productSchema2 = ProductSchema.builder().id(productSchemaId2).build();

        Product product1 = Product.builder().id(productId1).schema(productSchema1).build();
        Product product2 = Product.builder().id(productId2).schema(productSchema2).build();

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .productIds(List.of(productId1, productId2))
                .maxAvailableSpace(500)
                .build();

        Supplier supplier = Supplier.builder()
                .id(supplierId)
                .inventory(inventory)
                .build();

        when(supplierRepository.findAll()).thenReturn(List.of(supplier));
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));

        //***** <-- when: Initialize stock for all inventories --> *****//
        inventoryService.initializeStockForAllInventories();

        //***** <-- then: Verify stock is initialized correctly --> *****//
        assertEquals(1, inventoryService.getStock().size());
        assertTrue(inventoryService.getStock().containsKey(inventoryId));

        Map<Long, List<Product>> inventoryStock = inventoryService.getStock().get(inventoryId);
        assertEquals(2, inventoryStock.size());
        assertTrue(inventoryStock.containsKey(productSchemaId1));
        assertTrue(inventoryStock.containsKey(productSchemaId2));

        assertEquals(1, inventoryStock.get(productSchemaId1).size());
        assertEquals(productId1, inventoryStock.get(productSchemaId1).get(0).getId());

        assertEquals(1, inventoryStock.get(productSchemaId2).size());
        assertEquals(productId2, inventoryStock.get(productSchemaId2).get(0).getId());
    }

    @Test
    void shouldCreateInventoryWhenDepotSpaceIsSufficient() {
        //***** <-- given: Sufficient depot space and valid request --> *****//
        int lowStockAlertThreshold = 50;
        int expiryAlertThreshold = 10;
        int reorderThreshold = 30;
        int reorderQuantity = 100;

        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .lowStockAlertThreshold(lowStockAlertThreshold)
                .expiryAlertThreshold(expiryAlertThreshold)
                .reorderThreshold(reorderThreshold)
                .reorderQuantity(reorderQuantity)
                .build();

        //***** <-- when: Create inventory is called --> *****//
        Inventory inventory = inventoryService.createInventory(request);

        //***** <-- then: Verify the created inventory --> *****//
        assertNotNull(inventory);
        assertEquals(0, inventory.getUsedSpace());
        assertEquals(1000, inventory.getMaxAvailableSpace());
        assertEquals(lowStockAlertThreshold, inventory.getLowStockAlertThreshold());
        assertEquals(expiryAlertThreshold, inventory.getExpiryAlertThreshold());
        assertEquals(reorderThreshold, inventory.getReorderThreshold());
        assertEquals(reorderQuantity, inventory.getReorderQuantity());
    }

    @Test
    void shouldThrowDepotFullExceptionWhenDepotSpaceIsInsufficient() {
        //***** <-- given: Insufficient depot space and valid request --> *****//
        ReflectionTestUtils.setField(inventoryService, "AVAILABLE_DEPOT_SPACE_FOR_NEW_INVENTORY", 1100);

        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .lowStockAlertThreshold(50)
                .expiryAlertThreshold(10)
                .reorderThreshold(30)
                .reorderQuantity(100)
                .build();

        inventoryService.createInventory(request);

        //***** <-- when: Create inventory is called --> *****//
        DepotFullException exception = assertThrows(
                DepotFullException.class,
                () -> inventoryService.createInventory(request)
        );

        //***** <-- then: Verify exception details --> *****//
        assertEquals(
                "Not enough space in depot for new inventory. Available space: 100, requested space: 1000",
                exception.getMessage()
        );
    }

    @Test
    void shouldUpdateInventorySuccessfully() {
        //***** <-- given: Existing inventory and valid request --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .lowStockAlertThreshold(10)
                .expiryAlertThreshold(5)
                .reorderThreshold(20)
                .reorderQuantity(50)
                .build();

        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .lowStockAlertThreshold(15)
                .expiryAlertThreshold(10)
                .reorderThreshold(25)
                .reorderQuantity(60)
                .build();

        //***** <-- when: updateInventory is called --> *****//
        Inventory updatedInventory = inventoryService.updateInventory(inventory, request);

        //***** <-- then: Verify inventory is updated --> *****//
        assertNotNull(updatedInventory);
        assertEquals(15, updatedInventory.getLowStockAlertThreshold());
        assertEquals(10, updatedInventory.getExpiryAlertThreshold());
        assertEquals(25, updatedInventory.getReorderThreshold());
        assertEquals(60, updatedInventory.getReorderQuantity());
    }

    @Test
    void shouldFetchInventoryByIdSuccessfully() {
        //***** <-- given: Existing inventory in the repository --> *****//
        Long inventoryId = 1L;
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .build();

        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));

        //***** <-- when: getInventoryById is called --> *****//
        Inventory fetchedInventory = inventoryService.getInventoryById(inventoryId);

        //***** <-- then: Verify inventory is fetched --> *****//
        assertNotNull(fetchedInventory);
        assertEquals(inventoryId, fetchedInventory.getId());
        verify(inventoryRepository, times(1)).findById(inventoryId);
    }

    @Test
    void shouldThrowInventoryNotFoundExceptionWhenInventoryDoesNotExist() {
        //***** <-- given: Inventory not present in the repository --> *****//
        Long inventoryId = 1L;
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.empty());

        //***** <-- when: getInventoryById is called --> *****//
        InventoryNotFoundException exception = assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getInventoryById(inventoryId)
        );

        //***** <-- then: Verify exception details --> *****//
        assertEquals("Inventory with ID 1 not found", exception.getMessage());
        verify(inventoryRepository, times(1)).findById(inventoryId);
    }

    @Test
    void shouldFetchInventoryStateBySupplierIdSuccessfully() {
        //***** <-- given: Existing inventory for the supplier and valid mapping --> *****//
        Long supplierId = 1L;
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        InventoryState inventoryState = new InventoryState(); // Mocked InventoryState
        when(inventoryRepository.findBySupplierId(supplierId)).thenReturn(Optional.of(inventory));
        when(reportService.mapToInventoryState(inventory)).thenReturn(inventoryState);

        //***** <-- when: getInventoryStateBySupplierId is called --> *****//
        InventoryState fetchedState = inventoryService.getInventoryStateBySupplierId(supplierId);

        //***** <-- then: Verify inventory state is fetched and mapped --> *****//
        assertNotNull(fetchedState);
        assertEquals(inventoryState, fetchedState);
        verify(inventoryRepository, times(1)).findBySupplierId(supplierId);
        verify(reportService, times(1)).mapToInventoryState(inventory);
    }

    @Test
    void shouldThrowInventoryNotFoundExceptionWhenInventoryForSupplierDoesNotExist() {
        //***** <-- given: Inventory not present for the supplier --> *****//
        Long supplierId = 1L;
        when(inventoryRepository.findBySupplierId(supplierId)).thenReturn(Optional.empty());

        //***** <-- when: getInventoryStateBySupplierId is called --> *****//
        InventoryNotFoundException exception = assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getInventoryStateBySupplierId(supplierId)
        );

        //***** <-- then: Verify exception details --> *****//
        assertEquals("Inventory for supplier with ID 1 not found", exception.getMessage());
        verify(inventoryRepository, times(1)).findBySupplierId(supplierId);
    }

    @Test
    void shouldFetchInventoryByProductIdSuccessfully() {
        //***** <-- given: Existing inventory for the product ID --> *****//
        Long productId = 101L;
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        //***** <-- when: getByProductId is called --> *****//
        Inventory fetchedInventory = inventoryService.getByProductId(productId);

        //***** <-- then: Verify inventory is fetched correctly --> *****//
        assertNotNull(fetchedInventory);
        assertEquals(1L, fetchedInventory.getId());
        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void shouldThrowInventoryNotFoundExceptionWhenInventoryForProductIdDoesNotExist() {
        //***** <-- given: No inventory for the product ID --> *****//
        Long productId = 101L;
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        //***** <-- when: getByProductId is called --> *****//
        InventoryNotFoundException exception = assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getByProductId(productId)
        );

        //***** <-- then: Verify exception details --> *****//
        assertEquals("Inventory for product with ID 101 not found", exception.getMessage());
        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void shouldFetchInventoriesWithStockForSchemaSuccessfully() {
        //***** <-- given: Inventories with stock for the product schema ID --> *****//
        Long productSchemaId = 202L;
        Inventory inventory1 = Inventory.builder().id(1L).build();
        Inventory inventory2 = Inventory.builder().id(2L).build();

        when(inventoryRepository.findAllByProductSchemaId(productSchemaId))
                .thenReturn(List.of(inventory1, inventory2));

        //***** <-- when: getInventoriesWithStockForSchema is called --> *****//
        List<Inventory> inventories = inventoryService.getInventoriesWithStockForSchema(productSchemaId);

        //***** <-- then: Verify inventories are fetched correctly --> *****//
        assertNotNull(inventories);
        assertEquals(2, inventories.size());
        assertTrue(inventories.stream().anyMatch(inv -> inv.getId().equals(1L)));
        assertTrue(inventories.stream().anyMatch(inv -> inv.getId().equals(2L)));
        verify(inventoryRepository, times(1)).findAllByProductSchemaId(productSchemaId);
    }

    @Test
    void shouldReturnEmptyListWhenNoInventoriesExistForProductSchemaId() {
        //***** <-- given: No inventories for the product schema ID --> *****//
        Long productSchemaId = 202L;
        when(inventoryRepository.findAllByProductSchemaId(productSchemaId)).thenReturn(Collections.emptyList());

        //***** <-- when: getInventoriesWithStockForSchema is called --> *****//
        List<Inventory> inventories = inventoryService.getInventoriesWithStockForSchema(productSchemaId);

        //***** <-- then: Verify result is empty --> *****//
        assertNotNull(inventories);
        assertTrue(inventories.isEmpty());
        verify(inventoryRepository, times(1)).findAllByProductSchemaId(productSchemaId);
    }

    @Nested
    class AddStockTests {
        @Test
        void shouldNotAddStockWithInvalidExpiration() {
            //***** <-- given: User with supplier and invalid expiry date --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .createdAt(LocalDateTime.now().plusDays(1)) // Inventory created in the future
                    .build();

            CreateProductStockRequest request = CreateProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(10)
                    .expiresAt(LocalDateTime.now().minusDays(1)) // Expiry date in the past
                    .build();

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

            //***** <-- when: addStock is called --> *****//
            InvalidProductExpiryException exception = assertThrows(
                    InvalidProductExpiryException.class,
                    () -> inventoryService.addStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals("Invalid expiry date: " + request.getExpiresAt() + ". Expiry date must be after inventory creation date.", exception.getMessage());
            verify(inventoryRepository, times(1)).findBySupplierId(1L);
        }

        @Test
        void shouldNotAddStockWithNegativeQuantity() {
            //***** <-- given: User with supplier and negative quantity --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            CreateProductStockRequest request = CreateProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(-5) // Invalid negative quantity
                    .expiresAt(LocalDateTime.now().plusDays(10))
                    .build();

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

            //***** <-- when: addStock is called --> *****//
            NonGreaterThanZeroQuantityException exception = assertThrows(
                    NonGreaterThanZeroQuantityException.class,
                    () -> inventoryService.addStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals("Requested quantity: -5, it must be greater than zero.", exception.getMessage());
            verify(inventoryRepository, times(1)).findBySupplierId(1L);
        }

        @Test
        void shouldNotAddStockForTooLargeAddStockRequest() {
            //***** <-- given: User with supplier and insufficient space --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .maxAvailableSpace(1000)
                    .usedSpace(900) // Only 100 space left
                    .createdAt(LocalDateTime.now())
                    .build();

            ProductSchema productSchema = ProductSchema.builder()
                    .id(101L)
                    .storageSpaceNeeded(20) // Each product needs 20 space
                    .build();

            CreateProductStockRequest request = CreateProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(10) // Needs 200 space
                    .expiresAt(LocalDateTime.now().plusDays(10))
                    .build();

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));
            when(productSchemaService.getProductSchemaById(101L)).thenReturn(productSchema);

            //***** <-- when: addStock is called --> *****//
            InventoryFullException exception = assertThrows(
                    InventoryFullException.class,
                    () -> inventoryService.addStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals("Not enough space in inventory for supplier ID: 1. Available space: 100, requested space: 200", exception.getMessage());
            verify(inventoryRepository, times(1)).findBySupplierId(1L);
        }

        @Test
        void shouldAddStock() {
            //***** <-- given: User with supplier and valid request --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .maxAvailableSpace(1000)
                    .supplier(Supplier.builder().id(1L).build())
                    .createdAt(LocalDateTime.now())
                    .productIds(new ArrayList<>())
                    .usedSpace(500) // 500 space available
                    .build();

            ProductSchema productSchema = ProductSchema.builder()
                    .id(101L)
                    .storageSpaceNeeded(20) // Each product needs 20 space
                    .name("Product A")
                    .build();

            int requestedQuantity = 10;
            CreateProductStockRequest request = CreateProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(requestedQuantity) // Needs 200 space
                    .expiresAt(LocalDateTime.now().plusDays(10))
                    .description("Test stock addition")
                    .build();

            List<Product> productsToAdd = IntStream.range(0, requestedQuantity)
                    .mapToObj(i -> Product.builder()
                            .schema(productSchema)
                            .supplierId(1L)
                            .description("Test stock addition")
                            .status(ProductStatus.FREE)
                            .expiresAt(request.getExpiresAt())
                            .build())
                    .collect(Collectors.toList());

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));
            when(productSchemaService.getProductSchemaById(101L)).thenReturn(productSchema);
            when(productRepository.saveAll(anyList())).thenReturn(productsToAdd);

            //***** <-- when: addStock is called --> *****//
            ProductStockResponse response = inventoryService.addStock(user, request);

            //***** <-- then: Verify stock is added successfully --> *****//
            assertNotNull(response);
            assertEquals(101L, response.getProductSchemaId());
            assertEquals(requestedQuantity, response.getQuantity());
            assertEquals("Stock added successfully", response.getResponse());

            var stock = inventoryService.getStock();
            assertTrue(stock.containsKey(1L));
            assertTrue(stock.get(1L).containsKey(101L));
            List<Product> storedProducts = stock.get(1L).get(101L);
            assertNotNull(storedProducts);
            assertEquals(requestedQuantity, storedProducts.size());

            for (Product product : storedProducts) {
                assertEquals(ProductStatus.FREE, product.getStatus());
                assertEquals(1L, product.getSupplierId());
                assertEquals(productSchema, product.getSchema());
            }

            verify(productRepository, times(1)).saveAll(anyList());
            verify(inventoryRepository, times(1)).save(inventory);

            int expectedUsedSpace = 500 + (requestedQuantity * productSchema.getStorageSpaceNeeded());
            assertEquals(expectedUsedSpace, inventory.getUsedSpace());
        }
    }

    @Nested
    class RemoveStockTest {

        @Test
        void shouldNotRemoveStockIfUserIsNotSupplier() {
            //***** <-- given: User without a supplier --> *****//
            MyUser user = MyUser.builder()
                    .userName("non_supplier_user")
                    .supplier(null) // No supplier associated
                    .build();

            RemoveProductStockRequest request = RemoveProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(5)
                    .build();

            //***** <-- when: removeStock is called --> *****//
            UserSupplierNotFoundException exception = assertThrows(
                    UserSupplierNotFoundException.class,
                    () -> inventoryService.removeStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals("User does not have a supplier", exception.getMessage());
        }

        @Test
        void shouldNotRemoveStockForStockWhichIsNotPresent() {
            //***** <-- given: User and inventory setup --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .build();

            RemoveProductStockRequest request = RemoveProductStockRequest.builder()
                    .productSchemaId(101L) // ProductSchema not present
                    .quantity(5)
                    .build();

            // Mock the behavior of the productSchemaService to throw an exception
            when(productSchemaService.getProductSchemaById(101L))
                    .thenThrow(new ProductSchemaNotFoundException("Product schema with ID 101 not found"));

            // Mock inventory repository to avoid unrelated failures
            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

            //***** <-- when: removeStock is called --> *****//
            ProductSchemaNotFoundException exception = assertThrows(
                    ProductSchemaNotFoundException.class,
                    () -> inventoryService.removeStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals("Product schema with ID 101 not found", exception.getMessage());
        }


        @Test
        void shouldNotRemoveStockWithNegativeQuantity() {
            //***** <-- given: Valid user and inventory but negative quantity --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .build();

            RemoveProductStockRequest request = RemoveProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(-5) // Invalid negative quantity
                    .build();

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

            //***** <-- when: removeStock is called --> *****//
            NonGreaterThanZeroQuantityException exception = assertThrows(
                    NonGreaterThanZeroQuantityException.class,
                    () -> inventoryService.removeStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals("Requested quantity: -5, it must be greater than zero.", exception.getMessage());
        }

        @Test
        void shouldNotRemoveForTooLargeRemoveStockRequest() {
            //***** <-- given: User and inventory but insufficient stock --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .build();

            List<Product> products = IntStream.range(0, 3) // Only 3 products available
                    .mapToObj(i -> Product.builder()
                            .status(ProductStatus.FREE)
                            .build())
                    .toList();

            var stock = Map.of(1L, Map.of(101L, products));
            ReflectionTestUtils.setField(inventoryService, "stock", stock);

            RemoveProductStockRequest request = RemoveProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(5) // Requesting more than available
                    .build();

            ProductSchema productSchema = ProductSchema.builder()
                    .id(101L)
                    .storageSpaceNeeded(20) // Each product needs 20 space
                    .name("Product")
                    .build();

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));
            when(productSchemaService.getProductSchemaById(101L)).thenReturn(productSchema);

            //***** <-- when: removeStock is called --> *****//
            InventoryOutOfStockException exception = assertThrows(
                    InventoryOutOfStockException.class,
                    () -> inventoryService.removeStock(user, request)
            );

            //***** <-- then: Verify exception details --> *****//
            assertEquals(
                    "Too few stock of: Product in inventory for supplier ID: 1. Available stock: 3, requested: 5",
                    exception.getMessage()
            );
        }

        @Test
        void shouldRemoveStock() {
            //***** <-- given: User and valid inventory with sufficient stock --> *****//
            MyUser user = MyUser.builder()
                    .userName("supplier_user")
                    .supplier(Supplier.builder().id(1L).build())
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .productIds(new ArrayList<>(List.of(1L,2L,3L)))
                    .build();

            ProductSchema productSchema = ProductSchema.builder()
                    .id(101L)
                    .name("Product A")
                    .build();

            int quantityToRemove = 3;
            List<Product> products = IntStream.range(0, quantityToRemove)
                    .mapToObj(i -> Product.builder()
                            .id(1L + i)
                            .schema(productSchema)
                            .status(ProductStatus.FREE)
                            .expiresAt(LocalDateTime.now().plusDays(i))
                            .build())
                    .toList();

            var stock = new HashMap<Long, Map<Long, List<Product>>>();
            stock.put(1L, new HashMap<>(Map.of(101L, new ArrayList<>(products))));
            ReflectionTestUtils.setField(inventoryService, "stock", stock);

            RemoveProductStockRequest request = RemoveProductStockRequest.builder()
                    .productSchemaId(101L)
                    .quantity(quantityToRemove)
                    .build();

            when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));
            when(productSchemaService.getProductSchemaById(101L)).thenReturn(productSchema);

            //***** <-- when: removeStock is called --> *****//
            ProductStockResponse response = inventoryService.removeStock(user, request);

            //***** <-- then: Verify stock removal --> *****//
            assertNotNull(response);
            assertEquals(101L, response.getProductSchemaId());
            assertEquals(quantityToRemove, response.getQuantity());
            assertEquals("Stock removed successfully", response.getResponse());

            verify(productRepository, times(1)).deleteAll(anyList());
            verify(inventoryRepository, times(1)).save(inventory);

            var remainingStock = inventoryService.getStock().get(1L).get(101L);
            assertNull(remainingStock); // because all stock is removed
        }
    }

    @Test
    void shouldReturnEmptyListWhenInventoryIdNotInStock() {
        //***** <-- given: User with supplier but inventory ID not in stock --> *****//
        MyUser user = MyUser.builder()
                .userName("supplier_user")
                .supplier(Supplier.builder().id(1L).build())
                .build();

        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>(); // No entry for inventory ID
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

        //***** <-- when: getAllProductsInInventoryForUser is called --> *****//
        List<Product> products = inventoryService.getAllProductsInInventoryForUser(user);

        //***** <-- then: Verify an empty list is returned --> *****//
        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    void shouldReturnAllProductsWhenInventoryIdExistsInStock() {
        //***** <-- given: User with supplier and inventory ID in stock --> *****//
        MyUser user = MyUser.builder()
                .userName("supplier_user")
                .supplier(Supplier.builder().id(1L).build())
                .build();

        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        List<Product> products = IntStream.range(0, 5)
                .mapToObj(i -> Product.builder()
                        .id((long) i)
                        .status(ProductStatus.FREE)
                        .build())
                .toList();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, products)); // Inventory ID exists in stock
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

        //***** <-- when: getAllProductsInInventoryForUser is called --> *****//
        List<Product> allProducts = inventoryService.getAllProductsInInventoryForUser(user);

        //***** <-- then: Verify all products are returned --> *****//
        assertNotNull(allProducts);
        assertEquals(5, allProducts.size());
        assertTrue(allProducts.containsAll(products));
    }

    @Test
    void shouldReserveOneProductSuccessfully() {
        //***** <-- given: Inventory and product setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        Product product = Product.builder()
                .id(1001L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .build();

        // Simulating the stock with the product
        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        // Mock repository interaction to save the updated product status
        doAnswer(invocation -> {
            Product updatedProduct = invocation.getArgument(0);
            updatedProduct.setStatus(ProductStatus.RESERVED);
            return updatedProduct;
        }).when(productRepository).save(any(Product.class));

        //***** <-- when: reserveOneProduct is called --> *****//
        inventoryService.reserveOneProduct(inventory, product);

        //***** <-- then: Verify product is reserved successfully --> *****//
        var updatedProducts = inventoryService.getStock().get(1L).get(101L);
        assertNotNull(updatedProducts);
        assertEquals(1, updatedProducts.size());
        assertEquals(ProductStatus.RESERVED, updatedProducts.get(0).getStatus());
        assertEquals(1001L, updatedProducts.get(0).getId());
    }

    @Test
    void shouldReserveProductsByClosestToExpiry() {
        //***** <-- given: Inventory, schema, and products setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        LocalDateTime now = LocalDateTime.now();

        // Products with different expiry dates
        Product product1 = Product.builder()
                .id(1001L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(5)) // Expiring in 5 days
                .build();

        Product product2 = Product.builder()
                .id(1002L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(2)) // Expiring in 2 days
                .build();

        Product product3 = Product.builder()
                .id(1003L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(10)) // Expiring in 10 days
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product1, product2, product3))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        int quantityToReserve = 2;

        // Mock repository interaction for updating product status
        doAnswer(invocation -> {
            Product updatedProduct = invocation.getArgument(0);
            updatedProduct.setStatus(ProductStatus.RESERVED);
            return updatedProduct;
        }).when(productRepository).save(any(Product.class));

        //***** <-- when: reserveProdByProdSupplName is called --> *****//
        List<Product> reservedProducts = inventoryService.reserveProdByProdSupplName(inventory, schema, quantityToReserve);

        //***** <-- then: Verify the correct products are reserved --> *****//
        assertNotNull(reservedProducts);
        assertEquals(quantityToReserve, reservedProducts.size());

        // Verify that the closest to expiry products are reserved
        assertEquals(1002L, reservedProducts.get(0).getId()); // Product with closest expiry (2 days)
        assertEquals(1001L, reservedProducts.get(1).getId()); // Next closest expiry (5 days)

        // Verify their status is updated in stock
        var updatedProducts = inventoryService.getStock().get(1L).get(101L);
        assertNotNull(updatedProducts);
        assertEquals(ProductStatus.RESERVED, updatedProducts.get(0).getStatus());
        assertEquals(ProductStatus.RESERVED, updatedProducts.get(1).getStatus());
    }

    @Test
    void shouldReserveProductsByProductNameSuccessfully() {
        //***** <-- given: Inventory, schema, and products setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        LocalDateTime now = LocalDateTime.now();

        // Products with different expiry dates
        Product product1 = Product.builder()
                .id(1001L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(5)) // Expiring in 5 days
                .build();

        Product product2 = Product.builder()
                .id(1002L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(2)) // Expiring in 2 days
                .build();

        Product product3 = Product.builder()
                .id(1003L)
                .schema(schema)
                .status(ProductStatus.RESERVED) // Already reserved
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product1, product2, product3))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        int requestedQuantity = 2;

        // Mock repository interaction for updating product status
        doAnswer(invocation -> {
            Product updatedProduct = invocation.getArgument(0);
            updatedProduct.setStatus(ProductStatus.RESERVED);
            return updatedProduct;
        }).when(productRepository).save(any(Product.class));

        //***** <-- when: reserveProdByProdName is called --> *****//
        List<Product> reservedProducts = inventoryService.reserveProdByProdName(inventory, schema, requestedQuantity);

        //***** <-- then: Verify the correct products are reserved --> *****//
        assertNotNull(reservedProducts);
        assertEquals(requestedQuantity, reservedProducts.size());

        // Verify that the closest to expiry products are reserved
        assertEquals(1002L, reservedProducts.get(0).getId()); // Product with closest expiry (2 days)
        assertEquals(1001L, reservedProducts.get(1).getId()); // Next closest expiry (5 days)

        // Verify their status is updated in stock
        var updatedProducts = inventoryService.getStock().get(1L).get(101L);
        assertNotNull(updatedProducts);

        // Reserved products should be updated
        assertEquals(ProductStatus.RESERVED, updatedProducts.get(0).getStatus());
        assertEquals(ProductStatus.RESERVED, updatedProducts.get(1).getStatus());

        // Product3 should remain unchanged
        assertEquals(ProductStatus.RESERVED, updatedProducts.get(2).getStatus());
    }

    @Test
    void shouldFreeProductsSuccessfully() {
        //***** <-- given: Products setup --> *****//
        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        Product product1 = Product.builder()
                .id(1001L)
                .supplierId(1L)
                .schema(schema)
                .status(ProductStatus.RESERVED)
                .build();

        Product product2 = Product.builder()
                .id(1002L)
                .supplierId(1L)
                .schema(schema)
                .status(ProductStatus.RESERVED)
                .build();

        List<Product> products = List.of(product1, product2);

        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product1, product2))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

        // Mock repository interaction for saving product status updates
        doAnswer(invocation -> {
            Product updatedProduct = invocation.getArgument(0);
            updatedProduct.setStatus(ProductStatus.FREE);
            return updatedProduct;
        }).when(productRepository).save(any(Product.class));

        //***** <-- when: freeProducts is called --> *****//
        inventoryService.freeProducts(products);

        //***** <-- then: Verify product statuses are updated --> *****//
        var updatedProducts = inventoryService.getStock().get(1L).get(101L);
        assertNotNull(updatedProducts);
        updatedProducts.forEach(product -> assertEquals(ProductStatus.FREE, product.getStatus()));
    }

    @Test
    void shouldRemoveCompletedOrderProductsSuccessfully() {
        //***** <-- given: Order products setup --> *****//
        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        Product product1 = Product.builder()
                .id(1001L)
                .supplierId(1L)
                .schema(schema)
                .status(ProductStatus.RESERVED)
                .build();

        Product product2 = Product.builder()
                .id(1002L)
                .supplierId(1L)
                .schema(schema)
                .status(ProductStatus.RESERVED)
                .build();

        List<Product> orderProducts = List.of(product1, product2);

        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product1, product2))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        when(inventoryRepository.findBySupplierId(1L)).thenReturn(Optional.of(inventory));

        // Mock repository interaction for saving product status updates
        doAnswer(invocation -> {
            Product updatedProduct = invocation.getArgument(0);
            updatedProduct.setStatus(ProductStatus.REMOVED);
            return updatedProduct;
        }).when(productRepository).save(any(Product.class));

        // Mock alertService interaction
        doNothing().when(alertService).checkStockForReorder(anyMap());

        //***** <-- when: removeCompletedOrderProducts is called --> *****//
        inventoryService.removeCompletedOrderProducts(orderProducts);

        //***** <-- then: Verify product statuses are updated --> *****//
        var updatedProducts = inventoryService.getStock().get(1L).get(101L);
        assertNotNull(updatedProducts);
        updatedProducts.forEach(product -> assertEquals(ProductStatus.REMOVED, product.getStatus()));

        // Verify alertService interaction
        verify(alertService, times(1)).checkStockForReorder(stock);
    }

    @Test
    void shouldChangeProductExpirationStatusSuccessfully() {
        //***** <-- given: Inventory, schema, and product setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        Product product = Product.builder()
                .id(1001L)
                .schema(schema)
                .expiryStatus(ExpiryStatus.NOTEXPIRED)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        // Mock repository interaction for saving product status updates
        doAnswer(invocation -> invocation.<Product>getArgument(0)).when(productRepository).save(any(Product.class));

        //***** <-- when: changeProductExpirationStatus is called with SOONTOEXPIRE --> *****//
        inventoryService.changeProductExpirationStatus(1L, 101L, 1001L, ExpiryStatus.SOONTOEXPIRE);

        //***** <-- then: Verify expiry status is updated --> *****//
        var updatedProduct = inventoryService.getStock().get(1L).get(101L).get(0);
        assertNotNull(updatedProduct);
        assertEquals(ExpiryStatus.SOONTOEXPIRE, updatedProduct.getExpiryStatus());

        //***** <-- when: changeProductExpirationStatus is called with EXPIRED --> *****//
        inventoryService.changeProductExpirationStatus(1L, 101L, 1001L, ExpiryStatus.EXPIRED);

        //***** <-- then: Verify expiry status is updated again --> *****//
        assertEquals(ExpiryStatus.EXPIRED, updatedProduct.getExpiryStatus());
    }

    @Test
    void shouldChangeProductStatusSuccessfully() {
        //***** <-- given: Inventory, schema, and product setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        Product product = Product.builder()
                .id(1001L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, new ArrayList<>(List.of(product))));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        // Mock repository interaction for saving product status updates
        doAnswer(invocation -> invocation.<Product>getArgument(0)).when(productRepository).save(any(Product.class));

        //***** <-- when: changeProductStatus is called with RESERVED --> *****//
        inventoryService.changeProductStatus(1L, 101L, 1001L, ProductStatus.RESERVED);

        //***** <-- then: Verify product status is updated --> *****//
        var updatedProduct = inventoryService.getStock().get(1L).get(101L).get(0);
        assertNotNull(updatedProduct);
        assertEquals(ProductStatus.RESERVED, updatedProduct.getStatus());

        //***** <-- when: changeProductStatus is called with REMOVED --> *****//
        inventoryService.changeProductStatus(1L, 101L, 1001L, ProductStatus.REMOVED);

        //***** <-- then: Verify product status is updated again --> *****//
        assertEquals(ProductStatus.REMOVED, updatedProduct.getStatus());
    }

    @Test
    void shouldCalculateCurrentStockBySchemaIdSuccessfully() {
        //***** <-- given: Stock with products under different schemas --> *****//
        ProductSchema schema1 = ProductSchema.builder()
                .id(101L)
                .build();

        ProductSchema schema2 = ProductSchema.builder()
                .id(102L)
                .build();

        Product product1 = Product.builder()
                .id(1001L)
                .schema(schema1)
                .status(ProductStatus.FREE) // Counted as available
                .build();

        Product product2 = Product.builder()
                .id(1002L)
                .schema(schema1)
                .status(ProductStatus.FREE) // Counted as available
                .build();

        Product product3 = Product.builder()
                .id(1003L)
                .schema(schema1)
                .status(ProductStatus.RESERVED) // Not counted
                .build();

        Product product4 = Product.builder()
                .id(1004L)
                .schema(schema2)
                .status(ProductStatus.FREE) // Different schema, not counted
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(
                101L, List.of(product1, product2, product3),
                102L, List.of(product4)
        ));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        //***** <-- when: getCurrentStockBySchemaId is called for schema1 --> *****//
        int availableStockSchema1 = inventoryService.getCurrentStockBySchemaId(101L);

        //***** <-- then: Verify stock is calculated correctly for schema1 --> *****//
        assertEquals(2, availableStockSchema1);

        //***** <-- when: getCurrentStockBySchemaId is called for schema2 --> *****//
        int availableStockSchema2 = inventoryService.getCurrentStockBySchemaId(102L);

        //***** <-- then: Verify stock is calculated correctly for schema2 --> *****//
        assertEquals(1, availableStockSchema2);
    }

    @Test
    void shouldReturnTrueIfReorderIsNeeded() {
        //***** <-- given: Inventory and product setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .reorderThreshold(5)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, List.of(
                Product.builder().status(ProductStatus.FREE).build(),
                Product.builder().status(ProductStatus.FREE).build() // Only 2 products available
        )));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        //***** <-- when: needsReorderForStock is called --> *****//
        boolean result = inventoryService.needsReorderForStock(inventory, 101L);

        //***** <-- then: Verify result --> *****//
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueIfLowOnStock() {
        //***** <-- given: Inventory and product setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .lowStockAlertThreshold(3)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, List.of(
                Product.builder().status(ProductStatus.FREE).build() // Only 1 product available
        )));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        //***** <-- when: lowOnStock is called --> *****//
        boolean result = inventoryService.lowOnStock(inventory, 101L);

        //***** <-- then: Verify result --> *****//
        assertTrue(result);
    }

    @Test
    void shouldReturnQuantityIfPositive() {
        //***** <-- when: validatePositiveQuantity is called with positive quantity --> *****//
        int result = inventoryService.validatePositiveQuantity(10);

        //***** <-- then: Verify result --> *****//
        assertEquals(10, result);
    }

    @Test
    void shouldThrowExceptionIfQuantityIsNonPositive() {
        //***** <-- when: validatePositiveQuantity is called with non-positive quantity --> *****//
        NonGreaterThanZeroQuantityException exception = assertThrows(
                NonGreaterThanZeroQuantityException.class,
                () -> inventoryService.validatePositiveQuantity(0) // Invalid quantity
        );

        //***** <-- then: Verify exception details --> *****//
        assertEquals("Requested quantity: 0, it must be greater than zero.", exception.getMessage());
    }

    @Test
    void shouldReturnProductWithSoonestExpiry() {
        //***** <-- given: Inventory, schema, and products setup --> *****//
        Inventory inventory = Inventory.builder()
                .id(1L)
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(101L)
                .build();

        LocalDateTime now = LocalDateTime.now();

        Product product1 = Product.builder()
                .id(1001L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(5)) // Expiring in 5 days
                .build();

        Product product2 = Product.builder()
                .id(1002L)
                .schema(schema)
                .status(ProductStatus.FREE)
                .expiresAt(now.plusDays(2)) // Expiring in 2 days
                .build();

        var stock = new HashMap<Long, Map<Long, List<Product>>>();
        stock.put(1L, Map.of(101L, List.of(product1, product2)));
        ReflectionTestUtils.setField(inventoryService, "stock", stock);

        //***** <-- when: getSoonestExpiryProduct is called --> *****//
        Product result = inventoryService.getSoonestExpiryProduct(inventory, schema);

        //***** <-- then: Verify the product with the closest expiry date is returned --> *****//
        assertNotNull(result);
        assertEquals(1002L, result.getId());
    }
}
