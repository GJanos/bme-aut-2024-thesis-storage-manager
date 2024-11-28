package com.bme.vik.aut.thesis.depot;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class StartupInitializationServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StartupInitializationService startupInitializationService;

    @Value("${custom.admin.username}")
    private String adminUsername;

    @Value("${custom.admin.password}")
    private String adminPassword;
    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        // Clear existing admin user if present
        userRepository.deleteAll();
        inventoryService.clearStock();
    }

    @Test
    void shouldCreateAdminUserIfNotPresent() {
        startupInitializationService.run();

        // Assert the admin user is created
        var adminUser = userRepository.findByUserName(adminUsername).orElseThrow();
        assertEquals(adminUsername, adminUser.getUsername());
        assertTrue(passwordEncoder.matches(adminPassword, adminUser.getPassword()));
        assertEquals(Role.ADMIN, adminUser.getRole());
    }

    @Test
    void shouldNotCreateAdminUserIfAlreadyPresent() {
        // Pre-create the admin user
        MyUser existingAdmin = MyUser.builder()
                .userName(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();
        userRepository.save(existingAdmin);

        startupInitializationService.run();

        // Assert no duplicate admin user exists
        var adminUsers = userRepository.findAll().stream()
                .filter(user -> user.getUsername().equals(adminUsername))
                .toList();
        assertEquals(1, adminUsers.size());
    }
}
