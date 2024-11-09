package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.info.dto.ProductResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.SupplierResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InfoServiceImpl implements InfoService {

    private static final Logger logger = LoggerFactory.getLogger(InfoServiceImpl.class);

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public UserResponse getUserInfoByName(String username) throws UsernameNotFoundException {
        logger.info("Fetching user info for username: {}", username);

        MyUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        logger.info("User info fetched successfully for username: {}", username);
        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        logger.info("Fetching all products information");
        List<Product> products = productRepository.findAll();
        logger.info("All products information fetched successfully");

        return products.stream().map(product -> ProductResponse.builder()
                .id(product.getId())
                .productName(product.getSchema().getName())
                .description(product.getDescription())
                .categories(product.getSchema().getCategories().stream().map(Category::getName).collect(Collectors.toList()))
                .status(product.getStatus().name())
                .expiresAt(product.getExpiresAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<SupplierResponse> getAllSuppliers() {
        logger.info("Fetching all suppliers information");
        List<Supplier> suppliers = supplierRepository.findAll();
        logger.info("All suppliers information fetched successfully");

        return suppliers.stream().map(supplier -> modelMapper.map(supplier, SupplierResponse.class)).collect(Collectors.toList());
    }

}
