package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import com.bme.vik.aut.thesis.depot.exception.inventory.DepotFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.product.InvalidProductExpiryException;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.supplier.InvalidCreateSupplierRequestException;
import com.bme.vik.aut.thesis.depot.exception.supplier.NonGreaterThanZeroQuantityException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.user.UserSupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.AlertService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.report.ReportService;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryState;
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
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.ProductStockResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.RemoveProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(supplierService, "SHOULD_GENERATE_RANDOM_PASSWORD", false);
    }

    @Test
    void shouldReturnAllSuppliersSuccessfully() {
        //***** <-- given: Mock supplier data --> *****//
        Supplier supplier1 = Supplier.builder()
                .id(1L)
                .name("Supplier A")
                .build();

        Supplier supplier2 = Supplier.builder()
                .id(2L)
                .name("Supplier B")
                .build();

        when(supplierRepository.findAll()).thenReturn(List.of(supplier1, supplier2));

        //***** <-- when: getAllSuppliers is called --> *****//
        List<Supplier> suppliers = supplierService.getAllSuppliers();

        //***** <-- then: Verify the result --> *****//
        assertNotNull(suppliers);
        assertEquals(2, suppliers.size());
        assertEquals("Supplier A", suppliers.get(0).getName());
        assertEquals("Supplier B", suppliers.get(1).getName());
        verify(supplierRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnSupplierByIdSuccessfully() {
        //***** <-- given: Mock supplier data --> *****//
        Supplier supplier = Supplier.builder()
                .id(1L)
                .name("Supplier A")
                .build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        //***** <-- when: getSupplierById is called --> *****//
        Supplier result = supplierService.getSupplierById(1L);

        //***** <-- then: Verify the result --> *****//
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Supplier A", result.getName());
        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSupplierNotFoundById() {
        //***** <-- given: Supplier not found --> *****//
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        //***** <-- when & then: getSupplierById throws exception --> *****//
        SupplierNotFoundException exception = assertThrows(
                SupplierNotFoundException.class,
                () -> supplierService.getSupplierById(1L)
        );

        assertEquals("Supplier with ID 1 not found", exception.getMessage());
        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnSupplierByNameSuccessfully() {
        //***** <-- given: Mock supplier data --> *****//
        Supplier supplier = Supplier.builder()
                .id(1L)
                .name("Supplier A")
                .build();

        when(supplierRepository.findByName("Supplier A")).thenReturn(Optional.of(supplier));

        //***** <-- when: getSupplierByName is called --> *****//
        Supplier result = supplierService.getSupplierByName("Supplier A");

        //***** <-- then: Verify the result --> *****//
        assertNotNull(result);
        assertEquals("Supplier A", result.getName());
        assertEquals(1L, result.getId());
        verify(supplierRepository, times(1)).findByName("Supplier A");
    }

    @Test
    void shouldThrowExceptionWhenSupplierNotFoundByName() {
        //***** <-- given: Supplier not found --> *****//
        when(supplierRepository.findByName("Nonexistent Supplier")).thenReturn(Optional.empty());

        //***** <-- when & then: getSupplierByName throws exception --> *****//
        SupplierNotFoundException exception = assertThrows(
                SupplierNotFoundException.class,
                () -> supplierService.getSupplierByName("Nonexistent Supplier")
        );

        assertEquals("Supplier with name Nonexistent Supplier not found", exception.getMessage());
        verify(supplierRepository, times(1)).findByName("Nonexistent Supplier");
    }

    @Nested
    class CreateSupplierTests {

        @Test
        void shouldThrowExceptionWhenSupplierAlreadyExists() {
            //***** <-- given: Supplier already exists --> *****//
            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Existing Supplier")
                    .build();

            when(supplierRepository.existsByName("Existing Supplier")).thenReturn(true);

            //***** <-- when & then: createSupplier throws exception --> *****//
            SupplierAlreadyExistsException exception = assertThrows(
                    SupplierAlreadyExistsException.class,
                    () -> supplierService.createSupplier(request)
            );

            assertEquals("Supplier with name Existing Supplier already exists", exception.getMessage());
            verify(supplierRepository, times(1)).existsByName("Existing Supplier");
        }

        @Test
        void shouldGenerateRandomPasswordWhenFlagIsTrue() {
            //***** <-- given: Random password generation enabled --> *****//
            ReflectionTestUtils.setField(supplierService, "SHOULD_GENERATE_RANDOM_PASSWORD", true);

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("New Supplier")
                    .lowStockAlertThreshold(10)
                    .expiryAlertThreshold(5)
                    .reorderThreshold(15)
                    .reorderQuantity(50)
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .build();

            Supplier supplier = Supplier.builder()
                    .name("New Supplier")
                    .inventory(inventory)
                    .build();

            when(supplierRepository.existsByName("New Supplier")).thenReturn(false);
            when(inventoryService.createInventory(request)).thenReturn(inventory);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(MyUser.class))).thenAnswer(invocation -> {
                MyUser user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtTokenService.generateToken(any(MyUser.class))).thenReturn("mockedJWT");

            //***** <-- when: createSupplier is called --> *****//
            SupplierCreationResponse response = supplierService.createSupplier(request);

            //***** <-- then: Verify response and interactions --> *****//
            assertNotNull(response);
            assertEquals("New Supplier", response.getUserName());
            assertNotNull(response.getGeneratedPassword()); // Random password is not null
            assertEquals("mockedJWT", response.getToken());
            verify(supplierRepository, times(1)).existsByName("New Supplier");
            verify(inventoryService, times(1)).createInventory(request);
            verify(passwordEncoder, times(1)).encode(anyString());

            // Verify password complexity (length or patterns)
            String generatedPassword = response.getGeneratedPassword();
            assertTrue(generatedPassword.length() >= 8); // Example complexity check
        }

        @Test
        void shouldCreateSupplierSuccessfullyWhenEverythingIsValid() {
            //***** <-- given: Valid request --> *****//
            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Valid Supplier")
                    .lowStockAlertThreshold(10)
                    .expiryAlertThreshold(5)
                    .reorderThreshold(15)
                    .reorderQuantity(50)
                    .build();

            Inventory inventory = Inventory.builder()
                    .id(1L)
                    .build();

            Supplier supplier = Supplier.builder()
                    .name("Valid Supplier")
                    .inventory(inventory)
                    .build();

            MyUser mockUser = MyUser.builder()
                    .id(1L)
                    .userName("Valid Supplier")
                    .password("encodedPassword")
                    .role(Role.SUPPLIER)
                    .supplier(supplier)
                    .build();

            when(supplierRepository.existsByName("Valid Supplier")).thenReturn(false);
            when(inventoryService.createInventory(request)).thenReturn(inventory);
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(userRepository.save(any(MyUser.class))).thenReturn(mockUser);
            when(jwtTokenService.generateToken(mockUser)).thenReturn("mockedJWT");

            //***** <-- when: createSupplier is called --> *****//
            SupplierCreationResponse response = supplierService.createSupplier(request);

            //***** <-- then: Verify response and interactions --> *****//
            assertNotNull(response);
            assertEquals("Valid Supplier", response.getUserName());
            assertEquals("password", response.getGeneratedPassword());
            assertEquals("mockedJWT", response.getToken());
            verify(supplierRepository, times(1)).existsByName("Valid Supplier");
            verify(inventoryService, times(1)).createInventory(request);
            verify(passwordEncoder, times(1)).encode("password");
            verify(userRepository, times(1)).save(any(MyUser.class));
        }

    }

    @Nested
    class UpdateSupplierTests {
        @Test
        void shouldThrowExceptionWhenSupplierNameAlreadyExists() {
            //***** <-- given: Existing supplier and conflicting request --> *****//
            Supplier existingSupplier = Supplier.builder()
                    .id(1L)
                    .name("Supplier A")
                    .build();

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Supplier B") // Conflicting name
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(existingSupplier));
            when(supplierRepository.existsByName("Supplier B")).thenReturn(true);

            //***** <-- when & then: updateSupplier throws exception --> *****//
            SupplierAlreadyExistsException exception = assertThrows(
                    SupplierAlreadyExistsException.class,
                    () -> supplierService.updateSupplier(1L, request)
            );

            assertEquals("Supplier with name Supplier B already exists", exception.getMessage());
            verify(supplierRepository, times(1)).existsByName("Supplier B");
            verify(supplierRepository, times(1)).findById(1L);
        }

        @Test
        void shouldThrowExceptionForInvalidLowStockThreshold() {
            //***** <-- given: Existing supplier and invalid request --> *****//
            Supplier existingSupplier = Supplier.builder()
                    .id(1L)
                    .name("Supplier A")
                    .build();

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Supplier A")
                    .lowStockAlertThreshold(-1) // Invalid value
                    .expiryAlertThreshold(5)
                    .reorderThreshold(10)
                    .reorderQuantity(20)
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(existingSupplier));
            when(supplierRepository.existsByName("Supplier A")).thenReturn(false);

            //***** <-- when & then: updateSupplier throws exception --> *****//
            InvalidCreateSupplierRequestException exception = assertThrows(
                    InvalidCreateSupplierRequestException.class,
                    () -> supplierService.updateSupplier(1L, request)
            );

            assertEquals("Low stock alert threshold must be greater than or equal to 0", exception.getMessage());
            verify(supplierRepository, times(1)).findById(1L);
        }

        @Test
        void shouldUpdateSupplierSuccessfullyWhenEverythingIsValid() {
            //***** <-- given: Existing supplier and valid request --> *****//
            Supplier existingSupplier = Supplier.builder()
                    .id(1L)
                    .name("Supplier A")
                    .inventory(Inventory.builder().id(1L).build())
                    .build();

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Supplier B") // Valid new name
                    .lowStockAlertThreshold(10)
                    .expiryAlertThreshold(5)
                    .reorderThreshold(15)
                    .reorderQuantity(50)
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(existingSupplier));
            when(supplierRepository.existsByName("Supplier B")).thenReturn(false);
            when(inventoryService.updateInventory(existingSupplier.getInventory(), request)).thenReturn(existingSupplier.getInventory());
            when(supplierRepository.save(existingSupplier)).thenReturn(existingSupplier);

            //***** <-- when: updateSupplier is called --> *****//
            Supplier updatedSupplier = supplierService.updateSupplier(1L, request);

            //***** <-- then: Verify updated supplier --> *****//
            assertNotNull(updatedSupplier);
            assertEquals("Supplier B", updatedSupplier.getName());
            verify(supplierRepository, times(1)).findById(1L);
            verify(supplierRepository, times(1)).existsByName("Supplier B");
            verify(inventoryService, times(1)).updateInventory(existingSupplier.getInventory(), request);
            verify(supplierRepository, times(1)).save(existingSupplier);
        }
    }

    @Test
    void shouldDeleteSupplierSuccessfully() {
        //***** <-- given: Existing supplier with user --> *****//
        MyUser user = MyUser.builder()
                .id(1L)
                .userName("SupplierUser")
                .build();

        Supplier supplier = Supplier.builder()
                .id(1L)
                .name("Supplier A")
                .user(user)
                .build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        //***** <-- when: deleteSupplier is called --> *****//
        supplierService.deleteSupplier(1L);

        //***** <-- then: Verify user is deleted --> *****//
        verify(userRepository, times(1)).delete(user);
        verify(supplierRepository, times(1)).findById(1L);

        // Verify no other interactions
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(supplierRepository);
    }

}
