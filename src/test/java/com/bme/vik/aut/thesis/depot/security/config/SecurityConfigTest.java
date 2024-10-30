package com.bme.vik.aut.thesis.depot.security.config;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.auth.AuthController;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtAuthFilter;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfig.class)
@WebAppConfiguration
public class  SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())  // Apply Spring Security’s mock configuration
                .build();
    }

    @SneakyThrows
    @Test
    void shouldPermitAllForWhiteListUrls() {
        String[] whiteListUrls = {
                "/auth/register",
                "/auth/authenticate",
                "/swagger-ui.html",
                "/v3/api-docs"
        };

        for (String url : whiteListUrls) {
            mockMvc.perform(MockMvcRequestBuilders.get(url))
                    .andExpect(status().isOk());
        }
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPermitAdminAccessForUserPath() {
        mockMvc.perform(MockMvcRequestBuilders.get("/user/"))
                .andExpect(status().isOk());

        int userID = 1;
        mockMvc.perform(MockMvcRequestBuilders.get("/user/" + userID))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.put("/user/" + userID))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.delete("/user/" + userID))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldDenyAccessToUserEndpointForNonAdmin() {
        mockMvc.perform(MockMvcRequestBuilders.get("/user/"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(authorities = "user:read")
    void shouldPermitAccessToInfoEndpointForUsersWithReadPermission() throws Exception {
        mockMvc.perform(get("/info/details"))
                .andExpect(status().isOk()); // Expect 200 OK for users with the required permission
    }

    @Test
    void shouldDenyAccessToProtectedUrlsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/protected-url"))
                .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized for non-authenticated users
    }

    @Test
    void shouldAuthenticateWithValidJwtToken() throws Exception {
        // Simulate a valid JWT token
        String validJwt = "your_valid_jwt_token_here";

        mockMvc.perform(get("/protected-endpoint")
                        .header("Authorization", "Bearer " + validJwt))
                .andExpect(status().isOk());

        verify(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void shouldRejectRequestWithInvalidJwtToken() throws Exception {
        // Simulate an invalid JWT token
        String invalidJwt = "invalid_jwt_token";

        mockMvc.perform(get("/protected-endpoint")
                        .header("Authorization", "Bearer " + invalidJwt))
                .andExpect(status().isUnauthorized());

        verify(jwtAuthFilter).doFilter(any(), any(), any());
    }
}
