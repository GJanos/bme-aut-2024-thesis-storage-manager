package com.bme.vik.aut.thesis.depot.general.supplier.product;

import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public Product getProductById(Long id) {
        logger.info("Fetching product by ID: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found"));
    }

    public boolean isProductAvailable(Product product) {
        return product.getStatus() == ProductStatus.FREE;
    }
}