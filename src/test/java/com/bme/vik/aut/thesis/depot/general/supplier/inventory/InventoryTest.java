package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

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
