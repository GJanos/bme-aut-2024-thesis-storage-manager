package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.exception.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) throws UserNotFoundByIDException {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Integer id, @RequestBody UserModifyRequest request) throws UserNotFoundByIDException {
        UserResponse updatedUser = userService.updateUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) throws UserNotFoundByIDException {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}