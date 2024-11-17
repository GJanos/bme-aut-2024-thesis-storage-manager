package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryEventListener inventoryEventListener;

    @Test
    void shouldHandleReorderEventAndAddStock() {
        //***** <-- given: A reorder event with inventory and product --> *****//
        Long inventoryId = 1L;
        Long productSchemaId = 10L;
        String productDescription = "Sample description";
        int reorderQuantity = 50;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        String productName = "Sample Product";

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .reorderQuantity(reorderQuantity)
                .supplier(Supplier.builder()
                        .user(MyUser.builder()
                                .id(100L)
                                .userName("supplierUser")
                                .build())
                        .build())
                .build();

        Product product = Product.builder()
                .id(200L)
                .schema(ProductSchema.builder()
                        .id(productSchemaId)
                        .name(productName)
                        .build())
                .description(productDescription)
                .expiresAt(expiresAt)
                .build();

        ReorderAlertEvent event = new ReorderAlertEvent(this, inventory, product);

        //***** <-- when: The reorder event is handled --> *****//
        inventoryEventListener.handleReorderEvent(event);

        //***** <-- then: Ensure inventoryService.addStock is called with correct data --> *****//
        CreateProductStockRequest expectedRequest = CreateProductStockRequest.builder()
                .productSchemaId(productSchemaId)
                .description(productDescription)
                .quantity(reorderQuantity)
                .expiresAt(expiresAt)
                .build();

        verify(inventoryService).addStock(eq(inventory.getSupplier().getUser()), eq(expectedRequest));
    }

    @Test
    void shouldHandleLowStockAlertEvent() {
        //***** <-- given: A low stock alert event --> *****//
        Long inventoryId = 1L;
        Long productId = 200L;
        String productName = "Low Stock Product";

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .build();

        Product product = Product.builder()
                .id(productId)
                .schema(ProductSchema.builder()
                        .name(productName)
                        .build())
                .build();

        LowStockAlertEvent event = new LowStockAlertEvent(this, inventory, product);

        //***** <-- when: The low stock alert event is handled --> *****//
        inventoryEventListener.handleLowStockAlertEvent(event);

        //***** <-- then: Verify logging logic executed without errors --> *****//
        // Since no logic is implemented, this test ensures no exceptions are thrown.
        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldHandleProductExpiredEvent() {
        //***** <-- given: A product expired event --> *****//
        Long inventoryId = 1L;
        Long productId = 300L;
        String productName = "Expired Product";

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .build();

        Product product = Product.builder()
                .id(productId)
                .schema(ProductSchema.builder()
                        .name(productName)
                        .build())
                .build();

        ProductExpiredAlertEvent event = new ProductExpiredAlertEvent(this, inventory, product);

        //***** <-- when: The product expired event is handled --> *****//
        inventoryEventListener.handleProductExpiredEvent(event);

        //***** <-- then: Verify logging logic executed without errors --> *****//
        // Since no logic is implemented, this test ensures no exceptions are thrown.
        verifyNoInteractions(inventoryService);
    }
}
