package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.exception.user.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldReturnEmptyUserListWhenNoUsersExist() {
        //***** <-- given: No users in repository --> *****//
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        //***** <-- when: Retrieve all users --> *****//
        List<UserResponse> users = userService.getAllUsers();

        //***** <-- then: Expect empty list --> *****//
        assertTrue(users.isEmpty(), "User list should be empty when no users exist");
        verify(userRepository).findAll();
    }

    @Test
    void shouldReturnAllUsersWhenMultipleUsersExist() {
        //***** <-- given: Multiple users in repository --> *****//
        List<MyUser> mockUsers = List.of(
                MyUser.builder().id(1).userName("user1").password("password1").role(Role.USER).build(),
                MyUser.builder().id(2).userName("user2").password("password2").role(Role.USER).build()
        );
        when(userRepository.findAll()).thenReturn(mockUsers);

        //***** <-- when: Retrieve all users --> *****//
        List<UserResponse> users = userService.getAllUsers();

        //***** <-- then: Expect list of users --> *****//
        assertEquals(2, users.size(), "User list should contain two users");
        verify(userRepository).findAll();
    }

    @Test
    void shouldReturnUserByIdWhenValidIdProvided() {
        //***** <-- given: User with valid ID --> *****//
        int userId = 1;
        String userName = "user1";
        String password = "password1";

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        MyUser existingUser = MyUser.builder()
                .id(userId)
                .userName(userName)
                .password(password)
                .role(Role.USER)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .userName(userName)
                .role(Role.USER)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(modelMapper.map(existingUser, UserResponse.class)).thenReturn(userResponse);

        //***** <-- when: Retrieve user by ID --> *****//
        UserResponse userResponseReal = userService.getUserById(userId);

        //***** <-- then: Verify returned user --> *****//
        assertNotNull(userResponseReal);
        assertEquals(userName, userResponseReal.getUserName());
        assertEquals(Role.USER, userResponseReal.getRole());

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowExceptionWhenRequestingUserAndInvalidIdProvided() {
        //***** <-- given: Invalid user ID --> *****//
        int invalidUserId = -1;
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to retrieve user by invalid ID --> *****//
        assertThrows(UserNotFoundByIDException.class, () -> userService.getUserById(invalidUserId));

        verify(userRepository).findById(invalidUserId);
    }

    @Test
    void shouldUpdateUserSuccessfullyWhenValidIdProvided() {
        //***** <-- given: Existing user with valid ID --> *****//
        int userId = 1;
        String userName = "user1";
        String password = "password1";
        String newUserName = "newUser";
        String newPassword = "newPassword";
        String encodedPassword = "encodedPassword";

        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        MyUser existingUser = MyUser.builder()
                .id(userId)
                .userName(userName)
                .password(password)
                .role(Role.USER)
                .createdAt(createdAt)
                .updatedAt(createdAt) // Initially, createdAt and updatedAt are the same
                .build();

        UserModifyRequest updateRequest = UserModifyRequest.builder()
                .userName(newUserName)
                .password(newPassword)
                .build();

        MyUser updatedUser = MyUser.builder()
                .id(userId)
                .userName(newUserName)
                .password(encodedPassword)
                .role(Role.USER)
                .createdAt(createdAt) // createdAt remains unchanged
                .updatedAt(updatedAt) // updatedAt is now the current time
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .userName(newUserName)
                .role(Role.USER)
                .createdAt(createdAt) // createdAt remains unchanged
                .updatedAt(updatedAt) // updatedAt is now the current time
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUserName(newUserName)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(MyUser.class))).thenReturn(updatedUser);
        when(modelMapper.map(any(MyUser.class), eq(UserResponse.class))).thenReturn(userResponse);

        //***** <-- when: Update user --> *****//
        UserResponse updatedUserResponse = userService.updateUser(userId, updateRequest);

        //***** <-- then: Verify update --> *****//
        assertNotNull(updatedUserResponse);
        assertEquals(newUserName, updatedUserResponse.getUserName());
        assertEquals(Role.USER, updatedUserResponse.getRole());

        //***** <-- then: Verify method calls --> *****//
        verify(userRepository).findById(userId);
        verify(userRepository).existsByUserName(newUserName);
        verify(passwordEncoder).encode(newPassword);

        // Use an ArgumentCaptor to capture the saved MyUser instance
        ArgumentCaptor<MyUser> userCaptor = ArgumentCaptor.forClass(MyUser.class);
        verify(userRepository).save(userCaptor.capture());

        MyUser savedUser = userCaptor.getValue();

        assertEquals(newUserName, savedUser.getUsername());
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(createdAt, savedUser.getCreatedAt(), "createdAt should not change on update");
        assertTrue(ChronoUnit.SECONDS.between(updatedAt, savedUser.getUpdatedAt()) < 1, "updatedAt should be recent");

        savedUser.setUpdatedAt(savedUser.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingUserWithInvalidId() {
        //***** <-- given: Non-existent user ID --> *****//
        int nonExistentUserId = 999;
        UserModifyRequest updateRequest = UserModifyRequest.builder().userName("newUser").password("newPassword").build();

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to update user with non-existent ID --> *****//
        assertThrows(UserNotFoundByIDException.class, () -> userService.updateUser(nonExistentUserId, updateRequest));

        verify(userRepository).findById(nonExistentUserId);
        verify(userRepository, never()).save(any(MyUser.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingUserWithDuplicateUserName() {
        //***** <-- given: Existing username in another user --> *****//
        int userId = 1;
        MyUser existingUser = MyUser.builder().id(userId).userName("user1").password("password1").role(Role.USER).build();
        UserModifyRequest updateRequest = new UserModifyRequest("existingUser", "newPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUserName(updateRequest.getUserName())).thenReturn(true);

        //***** <-- when & then: Attempt to update with duplicate username --> *****//
        assertThrows(UserNameAlreadyExistsException.class, () -> userService.updateUser(userId, updateRequest));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(MyUser.class));
    }

    @Test
    void shouldDeleteUserByIdWhenValidIdProvided() {
        //***** <-- given: Existing user ID --> *****//
        int userId = 1;
        MyUser existingUser = MyUser.builder().id(userId).userName("user1").password("password1").role(Role.USER).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).delete(existingUser);

        //***** <-- when: Delete user by ID --> *****//
        userService.deleteUser(userId);

        //***** <-- then: Verify deletion --> *****//
        verify(userRepository).findById(userId);
        verify(userRepository).delete(existingUser);
    }

    @Test
    void shouldThrowExceptionWhenDeletingUserWithInvalidId() {
        //***** <-- given: Non-existent user ID --> *****//
        int nonExistentUserId = 999;

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to delete with invalid ID --> *****//
        assertThrows(UserNotFoundByIDException.class, () -> userService.deleteUser(nonExistentUserId));

        verify(userRepository).findById(nonExistentUserId);
        verify(userRepository, never()).delete(any());
    }
}
