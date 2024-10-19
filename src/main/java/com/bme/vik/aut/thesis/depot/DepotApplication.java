package com.bme.vik.aut.thesis.depot;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class DepotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepotApplication.class, args);
    }

    // TODO: cmd line runner here adding admin user to database from secure properties
    //do the code here
    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                    @Value("${custom.admin.username}") String adminUsername,
                                    @Value("${custom.admin.password}") String adminPassword) {
        return args -> {
            if (userRepository.findByUserName(adminUsername).isEmpty()) {
                MyUser adminUser = MyUser.builder()
                        .userName(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(adminUser);
            }
        };
    }
}
