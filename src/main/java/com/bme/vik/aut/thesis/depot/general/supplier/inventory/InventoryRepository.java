package com.bme.vik.aut.thesis.depot.general.supplier.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findBySupplierId(Long supplierId);

    @Query("SELECT i FROM Inventory i WHERE :productId MEMBER OF i.productIds")
    Optional<Inventory> findByProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT i FROM Inventory i JOIN Product p ON p.id MEMBER OF i.productIds WHERE p.schema.id = :productSchemaId")
    List<Inventory> findAllByProductSchemaId(@Param("productSchemaId") Long productSchemaId);
}