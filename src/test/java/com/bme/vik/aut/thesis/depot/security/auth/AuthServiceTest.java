package com.bme.vik.aut.thesis.depot.security.auth;

import com.bme.vik.aut.thesis.depot.exception.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.UserNameOrPasswordIsEmptyException;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthResponse;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        //***** <-- given --> *****//
        RegisterRequest request = new RegisterRequest("testuser", "password");
        MyUser mockUser = MyUser.builder()
                .userName(request.getUserName())
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.existsByUserName(request.getUserName())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(MyUser.class))).thenReturn(mockUser);
        when(jwtTokenService.generateToken(mockUser)).thenReturn("dummyToken");

        //***** <-- when --> *****//
        AuthResponse response = authService.register(request);

        //***** <-- then --> *****//
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());

        verify(userRepository).existsByUserName(request.getUserName());
        verify(userRepository).save(any(MyUser.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNameAlreadyExists() {
        //***** <-- given --> *****//
        RegisterRequest request = new RegisterRequest("existinguser", "password");
        when(userRepository.existsByUserName(request.getUserName())).thenReturn(true);

        //***** <-- when & then --> *****//
        assertThrows(UserNameAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(MyUser.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNameIsEmpty() {
        //***** <-- given: Empty username --> *****//
        RegisterRequest request = new RegisterRequest("", "password");

        //***** <-- when & then --> *****//
        assertThrows(UserNameOrPasswordIsEmptyException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(MyUser.class));
    }

    @Test
    void shouldThrowExceptionWhenUserPasswordIsEmpty() {
        //***** <-- given: Empty password --> *****//
        RegisterRequest request = new RegisterRequest("testuser", "");

        //***** <-- when & then --> *****//
        assertThrows(UserNameOrPasswordIsEmptyException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(MyUser.class));
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        //***** <-- given: Valid authentication request --> *****//
        AuthRequest request = new AuthRequest("testuser", "password");
        MyUser mockUser = MyUser.builder()
                .userName(request.getUserName())
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByUserName(request.getUserName())).thenReturn(Optional.of(mockUser));
        when(jwtTokenService.generateToken(mockUser)).thenReturn("dummyToken");

        //***** <-- when --> *****//
        AuthResponse response = authService.authenticate(request);

        //***** <-- then --> *****//
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenService).generateToken(mockUser);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringAuthentication() {
        //***** <-- given: Non-existent user --> *****//
        AuthRequest request = new AuthRequest("nonexistentuser", "password");
        when(userRepository.findByUserName(request.getUserName())).thenReturn(Optional.empty());

        //***** <-- when & then --> *****//
        assertThrows(UsernameNotFoundException.class, () -> authService.authenticate(request));

        verify(jwtTokenService, never()).generateToken(any(MyUser.class));
    }

    @Test
    void shouldFailAuthentication() {
        //***** <-- given: Invalid credentials --> *****//
        AuthRequest request = new AuthRequest("testuser", "wrongpassword");
        doThrow(new AuthenticationException("Authentication failed") {}).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        //***** <-- when & then --> *****//
        assertThrows(AuthenticationException.class, () -> authService.authenticate(request));

        verify(jwtTokenService, never()).generateToken(any(MyUser.class));
    }
}
