package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private InfoServiceImpl infoService;

    @Test
    void shouldFetchUserInfoSuccessfully() {
        //***** <-- given: Existing user in the repository --> *****//
        String username = "existingUser";
        MyUser mockUser = MyUser.builder()
                .userName(username)
                .role(Role.USER)
                .build();
        UserResponse expectedResponse = new UserResponse(1, username, Role.USER, LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByUserName(username)).thenReturn(Optional.of(mockUser));
        when(modelMapper.map(mockUser, UserResponse.class)).thenReturn(expectedResponse);

        //***** <-- when: Fetch user info --> *****//
        UserResponse actualResponse = infoService.getUserInfoByName(username);

        //***** <-- then: Validate response --> *****//
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getUserName(), actualResponse.getUserName());
        assertEquals(expectedResponse.getRole(), actualResponse.getRole());

        verify(userRepository).findByUserName(username);
        verify(modelMapper).map(mockUser, UserResponse.class);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        //***** <-- given: Non-existing user in the repository --> *****//
        String username = "nonExistentUser";

        when(userRepository.findByUserName(username)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to fetch user info, expect exception --> *****//
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> infoService.getUserInfoByName(username));

        assertEquals("User not found with username: " + username, exception.getMessage());
        verify(userRepository).findByUserName(username);
        verify(modelMapper, never()).map(any(MyUser.class), eq(UserResponse.class));
    }
}
