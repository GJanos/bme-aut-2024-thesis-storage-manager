package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
class InventoryTest {

    @Test
    void shouldAddStockCorrectly() {
        //***** <-- given: Prepare test data --> *****//
        Long productId1 = 1L;
        Long productId2 = 2L;
        int storageSpace1 = 10;
        int storageSpace2 = 20;

        Product product1 = Product.builder()
                .id(productId1)
                .schema(ProductSchema.builder().storageSpaceNeeded(storageSpace1).build())
                .build();

        Product product2 = Product.builder()
                .id(productId2)
                .schema(ProductSchema.builder().storageSpaceNeeded(storageSpace2).build())
                .build();

        Inventory inventory = Inventory.builder()
                .usedSpace(0)
                .maxAvailableSpace(100)
                .productIds(new ArrayList<>())
                .build();

        List<Product> productsToAdd = List.of(product1, product2);

        //***** <-- when: Add stock to inventory --> *****//
        inventory.addStock(productsToAdd);

        //***** <-- then: Verify inventory state --> *****//
        assertEquals(2, inventory.getProductIds().size());
        assertTrue(inventory.getProductIds().contains(productId1));
        assertTrue(inventory.getProductIds().contains(productId2));
        assertEquals(storageSpace1 + storageSpace2, inventory.getUsedSpace());
    }

    @Test
    void shouldRemoveStockCorrectly() {
        //***** <-- given: Prepare test data --> *****//
        Long productId1 = 1L;
        Long productId2 = 2L;
        int storageSpace1 = 10;
        int storageSpace2 = 20;

        Product product1 = Product.builder()
                .id(productId1)
                .schema(ProductSchema.builder().storageSpaceNeeded(storageSpace1).build())
                .build();

        Product product2 = Product.builder()
                .id(productId2)
                .schema(ProductSchema.builder().storageSpaceNeeded(storageSpace2).build())
                .build();

        Inventory inventory = Inventory.builder()
                .usedSpace(storageSpace1 + storageSpace2)
                .maxAvailableSpace(100)
                .productIds(new ArrayList<>(List.of(productId1, productId2)))
                .build();

        List<Product> productsToRemove = List.of(product1);

        //***** <-- when: Remove stock from inventory --> *****//
        inventory.removeStock(productsToRemove);

        //***** <-- then: Verify inventory state --> *****//
        assertEquals(1, inventory.getProductIds().size());
        assertFalse(inventory.getProductIds().contains(productId1));
        assertTrue(inventory.getProductIds().contains(productId2));
        assertEquals(storageSpace2, inventory.getUsedSpace());
    }

    @Test
    void shouldReturnTrueWhenSpaceIsAvailable() {
        //***** <-- given: Prepare inventory --> *****//
        int usedSpace = 30;
        int maxSpace = 100;
        int requiredSpace = 50;

        Inventory inventory = Inventory.builder()
                .usedSpace(usedSpace)
                .maxAvailableSpace(maxSpace)
                .build();

        //***** <-- when: Check available space --> *****//
        boolean result = inventory.hasAvailableSpace(requiredSpace);

        //***** <-- then: Verify result --> *****//
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenSpaceIsUnavailable() {
        //***** <-- given: Prepare inventory --> *****//
        int usedSpace = 80;
        int maxSpace = 100;
        int requiredSpace = 30;

        Inventory inventory = Inventory.builder()
                .usedSpace(usedSpace)
                .maxAvailableSpace(maxSpace)
                .build();

        //***** <-- when: Check available space --> *****//
        boolean result = inventory.hasAvailableSpace(requiredSpace);

        //***** <-- then: Verify result --> *****//
        assertFalse(result);
    }
}
