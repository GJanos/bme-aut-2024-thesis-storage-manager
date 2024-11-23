package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import static org.junit.jupiter.api.Assertions.*;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.*;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class SupplierRepositoryTest {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        //***** <-- given: Set up test data --> *****//
        TestUtil.createSupplier(
                userRepository,
                10,
                5,
                5,
                50,
                "Supplier A",
                "passwordA",
                passwordEncoder
        );

        TestUtil.createSupplier(
                userRepository,
                15,
                7,
                8,
                60,
                "Supplier B",
                "passwordB",
                passwordEncoder
        );
    }

    @AfterEach
    void tearDown() {
        supplierRepository.deleteAll();
    }

    //***** <-- Tests for existsByName --> *****//

    @Test
    void shouldReturnTrueWhenSupplierExistsByName() {
        //***** <-- when: Check existence by name --> *****//
        boolean exists = supplierRepository.existsByName("Supplier A");

        //***** <-- then: Verify result --> *****//
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseWhenSupplierDoesNotExistByName() {
        //***** <-- when: Check existence by non-existent name --> *****//
        boolean exists = supplierRepository.existsByName("Nonexistent Supplier");

        //***** <-- then: Verify result --> *****//
        assertFalse(exists);
    }

    //***** <-- Tests for findByName --> *****//

    @Test
    void shouldReturnSupplierWhenFoundByName() {
        //***** <-- when: Find supplier by name --> *****//
        Optional<Supplier> supplier = supplierRepository.findByName("Supplier B");

        //***** <-- then: Verify result --> *****//
        assertTrue(supplier.isPresent());
        assertEquals("Supplier B", supplier.get().getName());
    }

    @Test
    void shouldReturnEmptyOptionalWhenSupplierNotFoundByName() {
        //***** <-- when: Find supplier by non-existent name --> *****//
        Optional<Supplier> supplier = supplierRepository.findByName("Nonexistent Supplier");

        //***** <-- then: Verify result --> *****//
        assertFalse(supplier.isPresent());
    }
}
