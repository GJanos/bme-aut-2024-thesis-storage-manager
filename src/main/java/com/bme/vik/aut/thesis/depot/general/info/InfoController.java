package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.exception.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/info")
@RequiredArgsConstructor
@Tag(name = "Information", description = "Operations related to retrieving user-specific information")
public class InfoController {

    private final InfoService infoService;

    @Operation(
            summary = "Get current authenticated user's information",
            description = "Fetches information about the currently authenticated user, based on the JWT token provided in the request.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User information successfully retrieved",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized, authentication token is missing or invalid",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/user/me")
    @PreAuthorize("hasAnyAuthority('user:read')")
    public ResponseEntity<UserResponse> getUserInfo(
            @Parameter(description = "Authentication object that holds the authenticated user's details", required = true)
            Authentication authentication) throws UserNotFoundByIDException {

        UserResponse userResponse = infoService.getCurrentUserInfo(authentication.getName());
        return ResponseEntity.ok(userResponse);
    }
}

