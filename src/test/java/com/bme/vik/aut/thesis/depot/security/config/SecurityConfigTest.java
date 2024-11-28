package com.bme.vik.aut.thesis.depot.security.config;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final Map<String, UrlConfig> WHITELISTED_URLS = Map.of(
            "/auth/register", new UrlConfig(HttpMethod.POST, RegisterRequest.class, HttpStatus.OK),
            "/auth/authenticate", new UrlConfig(HttpMethod.POST, AuthRequest.class, HttpStatus.OK),
            "/swagger-ui.html", new UrlConfig(HttpMethod.GET, null, HttpStatus.FOUND),
            "/swagger-ui/index.html", new UrlConfig(HttpMethod.GET, null, HttpStatus.OK),
            "/v3/api-docs", new UrlConfig(HttpMethod.GET, null, HttpStatus.OK)
    );

    @Value("${custom.admin.username}")
    private String adminUsername;

    @Value("${custom.admin.password}")
    private String adminPassword;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        TestUtil.createAndRegisterUser(
                userRepository,
                adminUsername,
                adminPassword,
                Role.ADMIN,
                authService,
                passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldPermitAllForWhiteListUrls() {
        WHITELISTED_URLS.forEach(this::performRequestAndVerify);
    }

    // Helper method to perform request based on config and verify status
    private void performRequestAndVerify(String url, UrlConfig config) {
        WebTestClient.RequestHeadersSpec<?> request = webTestClient
                .method(config.method)
                .uri(url);

        // Conditionally add request body if defined
        if (config.bodyClass != null && config.method == HttpMethod.POST) {
            ((WebTestClient.RequestBodySpec) request).bodyValue(getSampleBody(config.bodyClass));
        }

        // Execute and verify the request with the expected status
        request.exchange()
                .expectStatus().isEqualTo(config.expectedStatus);
    }

    // Helper method to return a sample body for each request type
    private Object getSampleBody(Class<?> bodyClass) {
        if (bodyClass == RegisterRequest.class) {
            return RegisterRequest.builder()
                    .userName("depotuser")
                    .password("depotuser")
                    .build();
        } else if (bodyClass == AuthRequest.class) {
            return AuthRequest.builder()
                    .userName(adminUsername)
                    .password(adminPassword)
                    .build();
        }
        return null;
    }

    // Inner class to hold URL configurations
    private static class UrlConfig {
        private final HttpMethod method;
        private final Class<?> bodyClass;
        private final HttpStatus expectedStatus;

        public UrlConfig(HttpMethod method, Class<?> bodyClass, HttpStatus expectedStatus) {
            this.method = method;
            this.bodyClass = bodyClass;
            this.expectedStatus = expectedStatus;
        }
    }
}
