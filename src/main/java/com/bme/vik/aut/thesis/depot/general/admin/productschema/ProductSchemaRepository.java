package com.bme.vik.aut.thesis.depot.general.admin.productschema;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSchemaRepository extends JpaRepository<ProductSchema, Long> {
    boolean existsByName(String name);
}
