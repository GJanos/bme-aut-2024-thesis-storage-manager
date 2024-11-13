package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.info.dto.OrderResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.ProductResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.SupplierResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

public interface InfoService {
    UserResponse getUserInfoByName(String username) throws UsernameNotFoundException;
    List<ProductResponse> getAllProducts();
    List<SupplierResponse> getAllSuppliers();
    List<OrderResponse> getUserOrders(Long userId);
}
