package com.bme.vik.aut.thesis.depot;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StartupInitializationService implements CommandLineRunner {

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
                user -> System.out.println("Admin user already exists."),
                () -> {
                    MyUser adminUser = MyUser.builder()
                            .userName(adminUsername)
                            .password(passwordEncoder.encode(adminPassword))
                            .role(Role.ADMIN)
                            .build();
                    userRepository.save(adminUser);
                    System.out.println("Admin user created successfully.");
                }
        );
    }
}
