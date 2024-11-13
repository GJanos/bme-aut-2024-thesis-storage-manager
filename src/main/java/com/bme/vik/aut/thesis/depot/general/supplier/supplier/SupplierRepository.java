package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByName(String name);

//    @Query("SELECT s FROM Supplier s JOIN s.inventory i WHERE :productId MEMBER OF i.productIds")
//    Supplier getByProductId(@Param("productId") Long productId);
    Optional<Supplier> findByName(String name);
}