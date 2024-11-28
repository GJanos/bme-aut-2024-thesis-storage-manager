package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductSchemaRepository productSchemaRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        //***** <-- given: Set up test data --> *****//
        Inventory inventory1 = Inventory.builder()
                .usedSpace(50)
                .maxAvailableSpace(100)
                .lowStockAlertThreshold(10)
                .expiryAlertThreshold(5)
                .reorderThreshold(15)
                .reorderQuantity(50)
                .productIds(List.of(4L, 5L))
                .build();

        Inventory inventory2 = Inventory.builder()
                .usedSpace(30)
                .maxAvailableSpace(80)
                .lowStockAlertThreshold(5)
                .expiryAlertThreshold(3)
                .reorderThreshold(10)
                .reorderQuantity(30)
                .productIds(List.of(6L))
                .build();

        inventoryRepository.saveAll(List.of(inventory1, inventory2));
    }

    @AfterEach
    void tearDown() {
        supplierRepository.deleteAll();
    }

    @Test
    void shouldReturnInventoryWhenFoundBySupplierId() {
        TestUtil.CreateSupplierResponse createSupplierResponse =
                TestUtil.createSupplier(userRepository,
                        10,
                        5,
                        5,
                        10,
                        "Test Supplier",
                        "password",
                        passwordEncoder);

        Supplier savedSupplier = createSupplierResponse.supplier();

        //***** <-- when: Find inventory by supplier ID --> *****//
        Optional<Inventory> foundInventory = inventoryRepository.findBySupplierId(savedSupplier.getId());

        //***** <-- then: Verify result --> *****//
        assertTrue(foundInventory.isPresent());
        assertEquals(savedSupplier.getId(), foundInventory.get().getSupplier().getId());
    }

    @Test
    void shouldNotReturnInventoryWhenNotFoundBySupplierId() {
        //***** <-- given: Non-existent supplier ID --> *****//
        Long supplierId = 999L;

        //***** <-- when: Find inventory by supplier ID --> *****//
        Optional<Inventory> foundInventory = inventoryRepository.findBySupplierId(supplierId);

        //***** <-- then: Verify result --> *****//
        assertFalse(foundInventory.isPresent());
    }

    @Test
    void shouldReturnInventoryWhenFoundByProductId() {
        //***** <-- given: Existing product ID --> *****//
        Long productId = 5L;

        //***** <-- when: Find inventory by product ID --> *****//
        Optional<Inventory> foundInventory = inventoryRepository.findByProductId(productId);

        //***** <-- then: Verify result --> *****//
        assertTrue(foundInventory.isPresent());
        assertTrue(foundInventory.get().getProductIds().contains(productId));
    }

    @Test
    void shouldNotReturnInventoryWhenNotFoundByProductId() {
        //***** <-- given: Non-existent product ID --> *****//
        Long productId = 999L;

        //***** <-- when: Find inventory by product ID --> *****//
        Optional<Inventory> foundInventory = inventoryRepository.findByProductId(productId);

        //***** <-- then: Verify result --> *****//
        assertFalse(foundInventory.isPresent());
    }

    @Test
    void shouldReturnInventoriesWhenFoundByProductSchemaId() {
        TestUtil.CreateProductResponse createProductResponse = TestUtil.createProduct(
                categoryRepository,
                productSchemaRepository,
                productRepository,
                List.of(new TestUtil.CategoryUnit("Category 1", "Test category")),
                "Test Product",
                10,
                "Test Product Description",
                1L,
                ProductStatus.FREE,
                LocalDateTime.now().plusDays(5)
        );
        Product savedProduct = createProductResponse.product();


        Inventory inventory = Inventory.builder()
                .usedSpace(30)
                .maxAvailableSpace(80)
                .lowStockAlertThreshold(5)
                .expiryAlertThreshold(3)
                .reorderThreshold(10)
                .reorderQuantity(30)
                .productIds(List.of(savedProduct.getId()))
                .build();

        inventoryRepository.save(inventory);

        //***** <-- when: Find inventories by product schema ID --> *****//
        List<Inventory> inventories = inventoryRepository.findAllByProductSchemaId(savedProduct.getSchema().getId());

        //***** <-- then: Verify result --> *****//
        assertEquals(1, inventories.size());
        assertTrue(inventories.get(0).getProductIds().contains(savedProduct.getId()));
    }

    @Test
    void shouldReturnEmptyListWhenNotFoundByProductSchemaId() {
        //***** <-- given: Non-existent product schema ID --> *****//
        Long productSchemaId = 999L;

        //***** <-- when: Find inventories by product schema ID --> *****//
        List<Inventory> inventories = inventoryRepository.findAllByProductSchemaId(productSchemaId);

        //***** <-- then: Verify result --> *****//
        assertEquals(0, inventories.size());
    }
}
