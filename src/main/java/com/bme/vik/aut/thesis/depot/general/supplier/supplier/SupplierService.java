package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import com.bme.vik.aut.thesis.depot.exception.supplier.InvalidCreateSupplierRequestException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);

    @Value("${custom.supplier.generate-random-password}")
    private boolean SHOULD_GENERATE_RANDOM_PASSWORD;

    private final InventoryService inventoryService;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public List<Supplier> getAllSuppliers() {
        logger.info("Fetching all suppliers");
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        logger.info("Fetching supplier by ID: {}", id);
        return supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier with ID " + id + " not found"));
    }

    public Supplier getSupplierByName(String name) {
        logger.info("Fetching supplier by name: {}", name);
        return supplierRepository.findByName(name)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier with name " + name + " not found"));
    }

    public SupplierCreationResponse createSupplier(CreateSupplierRequest request) {
        logger.info("Creating new supplier with name: {}", request.getName());

        if (supplierRepository.existsByName(request.getName())) {
            throw new SupplierAlreadyExistsException("Supplier with name " + request.getName() + " already exists");
        }

        verifySupplierRequest(request);

        // Create Inventory
        Inventory inventory = inventoryService.createInventory(request);

        // Create Supplier
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .inventory(inventory)
                .build();

        String randomPassword;
        if (SHOULD_GENERATE_RANDOM_PASSWORD) {
            randomPassword = generateRandomSupplierUserPassword();
        } else {
            randomPassword = "password";
        }

        MyUser user = MyUser.builder()
                .userName(supplier.getName())
                .password(passwordEncoder.encode(randomPassword))
                .role(Role.SUPPLIER)
                .supplier(supplier)
                .build();

        inventory.setSupplier(supplier);
        supplier.setUser(user);

        MyUser savedUser = userRepository.save(user);

        logger.info("Supplier created. User ID: {}, supplier ID: {}, inventory ID: {}", savedUser.getId(), supplier.getId(), inventory.getId());

        // Create JWT token
        String token = jwtTokenService.generateToken(savedUser);

        return SupplierCreationResponse.builder()
                .userName(savedUser.getUsername())
                .generatedPassword(randomPassword)
                .token(token)
                .supplier(supplier)
                .build();
    }

    public Supplier updateSupplier(Long id, CreateSupplierRequest request) {
        logger.info("Updating supplier with ID: {}", id);
        Supplier supplier = getSupplierById(id);

        if (supplierRepository.existsByName(request.getName()) && !supplier.getName().equals(request.getName())) {
            throw new SupplierAlreadyExistsException("Supplier with name " + request.getName() + " already exists");
        }

        verifySupplierRequest(request);

        supplier.setName(request.getName());
        inventoryService.updateInventory(supplier.getInventory(), request);

        Supplier updatedSupplier = supplierRepository.save(supplier);
        logger.info("Supplier with ID {} updated successfully", updatedSupplier.getId());

        return updatedSupplier;
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        MyUser user = supplier.getUser();
        logger.info("Deleting Supplier user with user ID: {} user name: {} and supplier ID: {}", user.getId(), user.getUsername(), supplier.getId());
        userRepository.delete(supplier.getUser());
    }

    private void verifySupplierRequest(CreateSupplierRequest request) {
        String errorMsg = null;
        if (request.getLowStockAlertThreshold() < 0) {
            errorMsg = "Low stock alert threshold must be greater than or equal to 0";
        }
        if (request.getExpiryAlertThreshold() < 0) {
            errorMsg = "Expiry alert threshold must be greater than or equal to 0";
        }
        if (request.getReorderThreshold() < 0) {
            errorMsg = "Reorder threshold must be greater than or equal to 0";
        }
        if (request.getReorderQuantity() < 0) {
            errorMsg = "Reorder quantity must be greater than or equal to 0";
        }
        if (errorMsg != null) {
            logger.error(errorMsg);
            throw new InvalidCreateSupplierRequestException(errorMsg);
        }
    }

    private String generateRandomSupplierUserPassword() {
        String upperCaseLetters = RandomStringUtils.random(2, 65, 90, true, true);
        String lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true);
        String numbers = RandomStringUtils.randomNumeric(2);
        String specialChar = RandomStringUtils.random(2, 33, 47, false, false);
        String totalChars = RandomStringUtils.randomAlphanumeric(2);
        String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
                .concat(numbers)
                .concat(specialChar)
                .concat(totalChars);
        List<Character> pwdChars = combinedChars.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(pwdChars);
        String password = pwdChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return password;
    }
}
