package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int storageSpaceNeeded;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_category_mapping",
            joinColumns = @JoinColumn(name = "product_schema_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Custom setter for storageSpaceNeeded to prevent negative values
    public void setStorageSpaceNeeded(int storageSpaceNeeded) {
        if (storageSpaceNeeded <= 0) {
            throw new NonGreaterThanZeroStorageSpaceException("Storage space needed must be greater than zero");
        }
        this.storageSpaceNeeded = storageSpaceNeeded;
    }

    // Custom builder to enforce storage space validation
    public static ProductSchemaBuilder builder() {
        return new CustomProductSchemaBuilder();
    }

    private static class CustomProductSchemaBuilder extends ProductSchemaBuilder {
        @Override
        public ProductSchemaBuilder storageSpaceNeeded(int storageSpaceNeeded) {
            if (storageSpaceNeeded <= 0) {
                throw new NonGreaterThanZeroStorageSpaceException("Storage space needed must be greater than zero");
            }
            return super.storageSpaceNeeded(storageSpaceNeeded);
        }
    }
}


