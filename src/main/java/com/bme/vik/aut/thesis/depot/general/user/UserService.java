package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Integer id) throws UserNotFoundByIDException;
    UserResponse updateUser(Integer id, UserModifyRequest request) throws UserNotFoundByIDException;
    void deleteUser(Integer id) throws UserNotFoundByIDException;
}
