package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.info.dto.ProductResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.SupplierResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
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

import java.util.List;

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

        UserResponse userResponse = infoService.getUserInfoByName(authentication.getName());
        return ResponseEntity.ok(userResponse);
    }

    @Operation(
            summary = "Get all products information",
            description = "Fetches all products information accessible by the user.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Products information successfully retrieved",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized, authentication token is missing or invalid",
                            content = @Content
                    )
            }
    )
    @GetMapping("/product")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = infoService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @Operation(
            summary = "Get supplier information",
            description = "Fetches the information of suppliers that the user has access to.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Suppliers information successfully retrieved",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SupplierResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized, authentication token is missing or invalid",
                            content = @Content
                    )
            }
    )
    @GetMapping("/supplier")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
        List<SupplierResponse> suppliers = infoService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

}

