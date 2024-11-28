package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
