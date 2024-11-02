package com.bme.vik.aut.thesis.depot.security.config;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.auth.AuthController;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtAuthFilter;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
