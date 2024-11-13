package com.bme.vik.aut.thesis.depot.general.admin.productschema;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSchemaRepository extends JpaRepository<ProductSchema, Long> {
    boolean existsByName(String name);
    Optional<ProductSchema> findByName(String name);
}
