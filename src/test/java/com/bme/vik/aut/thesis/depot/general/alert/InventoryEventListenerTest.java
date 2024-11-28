package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InventoryEventListener inventoryEventListener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryEventListener, "SHOULD_SEND_EMAIL_NOTIFICATION", true);
    }

    @Test
    void shouldHandleReorderEventAndAddStock() {
        //***** <-- given: A reorder event with inventory and reorders --> *****//
        Long inventoryId = 1L;
        Long productSchemaId = 10L;
        String productDescription = "Sample description";
        int reorderQuantity = 50;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        String productName = "Sample Product";
        String supplierEmail = "supplier@example.com";

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .reorderQuantity(reorderQuantity)
                .supplier(Supplier.builder()
                        .email(supplierEmail)
                        .user(MyUser.builder()
                                .id(100L)
                                .userName("supplierUser")
                                .build())
                        .build())
                .build();

        InternalReorder reorder = new InternalReorder(
                ProductSchema.builder().id(productSchemaId).name(productName).build(),
                productDescription,
                expiresAt
        );

        ReorderAlertEvent event = new ReorderAlertEvent(this, inventory, List.of(reorder));

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

        //***** <-- then: Ensure emailService.sendEmail is called --> *****//
        String expectedEmailBody = String.format(
                "Dear Supplier,\n\nAuto reorder triggered.\n\n%d new products added to the %s (id: %d) product schema's stock.\n\nThank you for your attention!",
                reorderQuantity, productName, productSchemaId
        );

        verify(emailService).sendEmail(eq(supplierEmail), eq("Reorder Notification"), eq(expectedEmailBody));
    }

    @Test
    void shouldHandleLowStockAlertEvent() {
        //***** <-- given: A low stock alert event --> *****//
        Long inventoryId = 1L;
        Long productSchemaId = 10L;
        Long productId1 = 200L;
        Long productId2 = 201L;
        String productName = "Low Stock Product";
        String supplierEmail = "supplier@example.com";

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder()
                        .email(supplierEmail)
                        .build())
                .build();

        ProductSchema productSchema = ProductSchema.builder()
                .id(productSchemaId)
                .name(productName)
                .build();

        Product product1 = Product.builder()
                .id(productId1)
                .schema(productSchema)
                .build();

        Product product2 = Product.builder()
                .id(productId2)
                .schema(productSchema)
                .build();

        Map<ProductSchema, List<Product>> lowStockProducts = Map.of(
                productSchema, List.of(product1, product2)
        );

        LowStockAlertEvent event = new LowStockAlertEvent(this, inventory, lowStockProducts);

        //***** <-- when: The low stock alert event is handled --> *****//
        inventoryEventListener.handleLowStockAlertEvent(event);

        //***** <-- then: Ensure emailService.sendEmail is called with correct data --> *****//
        String expectedEmailBody = "Dear Supplier,\n\n" +
                "The following products in your inventory are running low on stock for the Low Stock Product (10) product schema:\n\n" +
                " - Product 'Low Stock Product' (ID: 200)\n" +
                " - Product 'Low Stock Product' (ID: 201)\n\n" +
                "Please take necessary actions.\n\nThank you!";

        verify(emailService).sendEmail(eq(supplierEmail), eq("Low Stock Alert"), eq(expectedEmailBody));

        // Verify no interactions with inventoryService as this test only deals with alerts
        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldHandleProductExpiredEvent() {
        //***** <-- given: A product expired event --> *****//
        Long inventoryId = 1L;
        Long productId1 = 300L;
        Long productId2 = 301L;
        String productName1 = "Expired Product 1";
        String productName2 = "Expired Product 2";
        String supplierEmail = "supplier@example.com";

        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .supplier(Supplier.builder()
                        .email(supplierEmail)
                        .build())
                .build();

        Product product1 = Product.builder()
                .id(productId1)
                .schema(ProductSchema.builder()
                        .name(productName1)
                        .build())
                .build();

        Product product2 = Product.builder()
                .id(productId2)
                .schema(ProductSchema.builder()
                        .name(productName2)
                        .build())
                .build();

        List<Product> expiredProducts = List.of(product1, product2);

        ProductExpiredAlertEvent event = new ProductExpiredAlertEvent(this, inventory, expiredProducts);

        //***** <-- when: The product expired event is handled --> *****//
        inventoryEventListener.handleProductExpiredEvent(event);

        //***** <-- then: Ensure emailService.sendEmail is called with correct data --> *****//
        String expectedEmailBody = "Dear Supplier,\n\n" +
                "The following products in your inventory have expired:\n\n" +
                " - Product 'Expired Product 1' (ID: 300)\n" +
                " - Product 'Expired Product 2' (ID: 301)\n\n" +
                "Please take necessary actions.\n\nThank you!";

        verify(emailService).sendEmail(eq(supplierEmail), eq("Products Expired Alert"), eq(expectedEmailBody));

        // Ensure no interactions with inventoryService as this test only deals with alerts
        verifyNoInteractions(inventoryService);
    }

}
