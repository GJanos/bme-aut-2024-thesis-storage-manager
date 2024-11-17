package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations related to user management")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get all users",
            description = "Fetches a list of all users registered in the system.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Users successfully fetched",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
                    )
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Get a user by ID",
            description = "Fetches the user details based on the provided ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User successfully found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID of the user to be fetched", required = true)
            @PathVariable Long id) throws UserNotFoundByIDException {

        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Update a user by ID",
            description = "Updates the user details based on the provided ID and new user information.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "User name already exists",
                            content = @Content
                    )
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:update')")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID of the user to be updated", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for updating the user",
                    required = true,
                    content = @Content
            )
            @RequestBody UserModifyRequest request) throws UserNotFoundByIDException {

        UserResponse updatedUser = userService.updateUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser);
    }

    @Operation(
            summary = "Delete a user by ID",
            description = "Deletes the user based on the provided ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "User deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:delete')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to be deleted", required = true)
            @PathVariable Long id) throws UserNotFoundByIDException {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
