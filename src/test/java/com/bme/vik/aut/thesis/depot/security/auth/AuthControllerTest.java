package com.bme.vik.aut.thesis.depot.security.auth;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.config.SecurityConfig;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtAuthFilter;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // AuthController and SecurityConfig dependencies
    @MockBean
    private AuthService authService;
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

    @Test
    void shouldAllowAccessToEverybodyOnAuthPath() throws Exception {
        // these are white-listed paths
        mockMvc.perform(post("/auth/register"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/authenticate"))
                .andExpect(status().isOk());
    }

    @Test
    void register() {
    }

    @Test
    void authenticate() {
    }
}