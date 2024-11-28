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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InventoryEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventListener.class);

    @Value("${custom.alert.mail.should-send}")
    private boolean SHOULD_SEND_EMAIL_NOTIFICATION;

    private final InventoryService inventoryService;
    private final EmailService emailService;

    public InventoryEventListener(InventoryService inventoryService, EmailService emailService) {
        this.inventoryService = inventoryService;
        this.emailService = emailService;
    }

    @EventListener
    @Async
    public void handleLowStockAlertEvent(LowStockAlertEvent event) {
        if (!SHOULD_SEND_EMAIL_NOTIFICATION) {
            logger.info("Low stock alert email notification is disabled. Skipping email notification.");
            return;
        }

        Inventory inventory = event.getInventory();
        Map<ProductSchema, List<Product>> lowStockProducts = event.getStock();
        Supplier supplier = inventory.getSupplier();

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Dear Supplier,\n\n");

        for (Map.Entry<ProductSchema, List<Product>> entry : lowStockProducts.entrySet()) {
            ProductSchema productSchema = entry.getKey();
            List<Product> products = entry.getValue();

            emailBody.append(String.format("The following products in your inventory are running low on stock for the %s (%d) product schema:\n\n", productSchema.getName(), productSchema.getId()));

            for (Product product : products) {
                emailBody.append(String.format(" - Product '%s' (ID: %d)\n", product.getSchema().getName(), product.getId()));
            }
            emailBody.append("\n"); // Ensure this newline is added only once per product schema group
        }

        emailBody.append("Please take necessary actions.\n\nThank you!");

        emailService.sendEmail(supplier.getEmail(), "Low Stock Alert", emailBody.toString());
        logger.info("Low stock alert email sent to {}", supplier.getEmail());
    }

    @EventListener
    @Async
    public void handleReorderEvent(ReorderAlertEvent event) {
        Inventory inventory = event.getInventory();
        List<InternalReorder> reorders = event.getReorders();
        Supplier supplier = inventory.getSupplier();
        MyUser user = supplier.getUser();
        StringBuilder emailBody = new StringBuilder();

        logger.info("Handling reorder event for inventory ID: {}", inventory.getId());

        emailBody.append("Dear Supplier,\n\nAuto reorder triggered.\n\n");

        for (InternalReorder reorder : reorders) {
            CreateProductStockRequest request = CreateProductStockRequest.builder()
                    .productSchemaId(reorder.getProductSchema().getId())
                    .description(reorder.getProductDescription())
                    .quantity(inventory.getReorderQuantity())
                    .expiresAt(reorder.getExpiresAt())
                    .build();

            inventoryService.addStock(user, request);

            // Corrected string formatting using String.format()
            emailBody.append(String.format(
                    "%d new products added to the %s (id: %d) product schema's stock.\n",
                    inventory.getReorderQuantity(),
                    reorder.getProductSchema().getName(),
                    reorder.getProductSchema().getId()
            ));
        }

        emailBody.append("\nThank you for your attention!");

        if (!SHOULD_SEND_EMAIL_NOTIFICATION) {
            logger.info("Reorder notification email is disabled. Skipping email notification.");
            return;
        }

        // Send email notification
        emailService.sendEmail(supplier.getEmail(), "Reorder Notification", emailBody.toString());
        logger.info("Reorder notification email sent to {}", supplier.getEmail());
    }

    @EventListener
    @Async
    public void handleProductExpiredEvent(ProductExpiredAlertEvent event) {
        if (!SHOULD_SEND_EMAIL_NOTIFICATION) {
            logger.info("Product expired alert email notification is disabled. Skipping email notification.");
            return;
        }

        Inventory inventory = event.getInventory();
        List<Product> products = event.getProducts();
        Supplier supplier = inventory.getSupplier();

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Dear Supplier,\n\nThe following products in your inventory have expired:\n\n");

        for (Product product : products) {
            emailBody.append(String.format(" - Product '%s' (ID: %d)\n", product.getSchema().getName(), product.getId()));
        }

        emailBody.append("\nPlease take necessary actions.\n\nThank you!");

        emailService.sendEmail(supplier.getEmail(), "Products Expired Alert", emailBody.toString());
        logger.info("Product expired alert email sent to {}", supplier.getEmail());
    }

}