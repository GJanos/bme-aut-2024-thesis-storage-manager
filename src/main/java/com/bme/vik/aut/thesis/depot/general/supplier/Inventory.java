package com.bme.vik.aut.thesis.depot.general.supplier;

import com.bme.vik.aut.thesis.depot.general.admin.Product;
import com.bme.vik.aut.thesis.depot.general.admin.ProductSchema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "inventory", cascade = CascadeType.ALL)
    private Supplier supplier;

    private int usedSpace;

    private int maxAvailableSpace;

    private int lowStockAlertThreshold;

    private int expiryAlertThreshold;

    private int reorderThreshold;

    private int reorderQuantity;

    @ElementCollection
    @CollectionTable(name = "inventory_products", joinColumns = @JoinColumn(name = "inventory_id"))
    @Column(name = "product_id")
    private List<Long> productIds = new ArrayList<>();

    @Transient
    private Map<ProductSchema, List<Product>> stock = new HashMap<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // TODO place this method in a service class
//    // Dynamically build the in-memory stock map on application startup or when needed
//    @PostLoad
//    public void initializeStock() {
//        stock.clear();
//        for (Long productId : productIds) {
//            Product product = findProductById(productId); // Fetch the product by its ID
//            ProductSchema schema = product.getSchema();
//            stock.computeIfAbsent(schema, k -> new ArrayList<>()).add(product);
//        }
//    }

//    // Add a product (update both in-memory structure and the list of product IDs)
//    public void addProduct(Product product) {
//        this.productIds.add(product.getId());
//        stock.computeIfAbsent(product.getSchema(), k -> new ArrayList<>()).add(product);
//        this.usedSpace += product.getSpaceRequired();
//    }
//
//    // Remove a product (update both in-memory structure and the list of product IDs)
//    public void removeProduct(Product product) {
//        this.productIds.remove(product.getId());
//        List<Product> productsInSchema = stock.get(product.getSchema());
//        if (productsInSchema != null) {
//            productsInSchema.remove(product);
//            if (productsInSchema.isEmpty()) {
//                stock.remove(product.getSchema());
//            }
//        }
//        this.usedSpace -= product.getSpaceRequired();
//    }
//
//    public boolean hasAvailableSpace(int requiredSpace) {
//        return (usedSpace + requiredSpace) <= maxAvailableSpace;
//    }
}
