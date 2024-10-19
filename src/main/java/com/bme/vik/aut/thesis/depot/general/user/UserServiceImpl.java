package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.exception.UserNameAlreadyExistsError;
import com.bme.vik.aut.thesis.depot.exception.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public List<UserResponse> getAllUsers() {
        logger.info("Fetching all users");
        List<MyUser> users = userRepository.findAll();
        logger.info("Found {} users", users.size());
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(Integer id) throws UserNotFoundByIDException {
        logger.info("Fetching user by ID: {}", id);
        MyUser user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User with ID {} not found", id);
                    return new UserNotFoundByIDException("User with ID " + id + " not found");
                });

        logger.info("User with ID {} found: {}", id, user.getUsername());
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public UserResponse updateUser(Integer id, UserModifyRequest request) throws UserNotFoundByIDException {
        logger.info("Updating user with ID: {}", id);
        MyUser user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User with ID {} not found", id);
                    return new UserNotFoundByIDException("User with ID " + id + " not found");
                });

        if (userRepository.existsByUserName(request.getUserName()) &&
                !user.getUsername().equals(request.getUserName())) {
            logger.warn("Username {} already exists", request.getUserName());
            throw new UserNameAlreadyExistsError("User name already exists");
        }

        user.setUserName(request.getUserName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        logger.info("User with ID {} updated successfully", id);
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public void deleteUser(Integer id) throws UserNotFoundByIDException {
        logger.info("Deleting user with ID: {}", id);
        MyUser user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User with ID {} not found", id);
                    return new UserNotFoundByIDException("User with ID " + id + " not found");
                });

        userRepository.delete(user);
        logger.info("User with ID {} deleted successfully", id);
    }
}
