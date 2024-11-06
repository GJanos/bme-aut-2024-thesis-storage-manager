package com.bme.vik.aut.thesis.depot.security.auth;

import com.bme.vik.aut.thesis.depot.exception.user.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNameOrPasswordIsEmptyException;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthResponse;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user: {}", request.getUserName());

        if (request.getUserName().isEmpty() || request.getPassword().isEmpty()) {
            logger.warn("Username or password is empty");
            throw new UserNameOrPasswordIsEmptyException("Username or password is empty");
        }

        if (userRepository.existsByUserName(request.getUserName())) {
            logger.warn("Username {} already exists", request.getUserName());
            throw new UserNameAlreadyExistsException("Username already exists");
        }

        MyUser user = MyUser.builder()
                .userName(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        logger.info("User {} registered successfully", request.getUserName());

        String jwtToken = jwtTokenService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        logger.info("Authenticating user: {}", request.getUserName());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> {
                    logger.warn("User with username {} not found", request.getUserName());
                    return new UsernameNotFoundException("User not found");
                });

        var jwtToken = jwtTokenService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
