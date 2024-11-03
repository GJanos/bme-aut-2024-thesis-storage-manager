package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ProductSchemaRepositoryTest {

    private static final String PRODUCT_SCHEMA_NAME = "productSchema1";
    private static final int STORAGE_SPACE_NEEDED = 10;

    @Autowired
    private ProductSchemaRepository productSchemaRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        //***** <-- given: Save a product schema to the repository --> *****//
        ProductSchema productSchema = ProductSchema.builder()
                .name(PRODUCT_SCHEMA_NAME)
                .storageSpaceNeeded(STORAGE_SPACE_NEEDED)
                .categories(Collections.emptyList())
                .build();

        productSchemaRepository.save(productSchema);
    }

    @AfterEach
    void tearDown() {
        productSchemaRepository.deleteAll();
    }

    @Test
    void shouldReturnProductSchemaWhenProductSchemaExists() {
        //***** <-- when: Check existence by name --> *****//
        boolean exists = productSchemaRepository.existsByName(PRODUCT_SCHEMA_NAME);

        //***** <-- then: Verify existence --> *****//
        assertThat(exists).isTrue();
    }

    @Test
    void shouldNotReturnProductSchemaWhenProductSchemaDoesNotExist() {
        //***** <-- when: Check existence by non-existing name --> *****//
        String nonExistentName = "nonExistentProductSchema";
        boolean exists = productSchemaRepository.existsByName(nonExistentName);

        //***** <-- then: Verify non-existence --> *****//
        assertThat(exists).isFalse();
    }
}