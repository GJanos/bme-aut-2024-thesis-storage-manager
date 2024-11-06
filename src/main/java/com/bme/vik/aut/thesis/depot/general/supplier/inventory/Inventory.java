package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

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
    @JsonBackReference
    private Supplier supplier;

    private int usedSpace;

    private int maxAvailableSpace;

    private int lowStockAlertThreshold;

    private int expiryAlertThreshold; // in days

    private int reorderThreshold;

    private int reorderQuantity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inventory_products", joinColumns = @JoinColumn(name = "inventory_id"))
    @Column(name = "product_id")
    private List<Long> productIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addStock(List<Product> products) {
        // available space check not needed here,
        // as it is done in the service layer
        for (Product product : products) {
            this.productIds.add(product.getId());
            this.usedSpace += product.getSchema().getStorageSpaceNeeded();
        }
    }

    public void removeStock(List<Product> products) {
        for (Product product : products) {
            this.productIds.remove(product.getId());
            this.usedSpace -= product.getSchema().getStorageSpaceNeeded();
        }
    }

    public boolean hasAvailableSpace(int requiredSpace) {
        return (usedSpace + requiredSpace) <= maxAvailableSpace;
    }
}
