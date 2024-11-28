package com.bme.vik.aut.thesis.depot;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class StartupInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupInitializationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InventoryService inventoryService;

    @Value("${custom.admin.username}")
    private String adminUsername;

    @Value("${custom.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        initializeAdminUser();
        inventoryService.initializeStockForAllInventories();
    }

    private void initializeAdminUser() {
        userRepository.findByUserName(adminUsername).ifPresentOrElse(
                user -> logger.info("Admin user already exists."),
                () -> {
                    MyUser adminUser = MyUser.builder()
                            .userName(adminUsername)
                            .password(passwordEncoder.encode(adminPassword))
                            .role(Role.ADMIN)
                            .build();
                    userRepository.save(adminUser);
                    logger.info("Admin user created successfully.");
                }
        );
    }
}
