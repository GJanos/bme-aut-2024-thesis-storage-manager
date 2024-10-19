package com.bme.vik.aut.thesis.depot.security.config;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.logging.Logger;

@Configuration
@RequiredArgsConstructor
public class DepotConfig {

    private static final Logger logger = Logger.getLogger(DepotConfig .class.getName());
    private final UserRepository userRepository;
    @Bean
    public UserDetailsService userDetailsService() {
        return userName -> {
            logger.info("Loading user by user name: " + userName);
            return userRepository.findByUserName(userName)
                    .orElseThrow(() -> new UsernameNotFoundException("No user was found"));
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        logger.info("Creating authentication provider");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        logger.info("Creating authentication manager");
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Creating password encoder");
        return new BCryptPasswordEncoder();
    }
}
