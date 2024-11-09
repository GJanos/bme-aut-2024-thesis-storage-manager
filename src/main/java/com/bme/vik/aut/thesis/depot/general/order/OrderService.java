package com.bme.vik.aut.thesis.depot.general.order;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.info.dto.ProductResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.SupplierResponse;
import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderWithProductIdRequest;
import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderWithProductSupplierRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Transactional
    public Order createOrderByProductId(MyUser user, List<CreateOrderWithProductIdRequest> orderItems) {
//        logger.info("Creating new order for user ID: {}", user.getId());
//
//        List<OrderItem> items = new ArrayList<>();
//        for (OrderItemRequest itemRequest : orderItems) {
//            Product product = productRepository.findById(itemRequest.getProductId())
//                    .orElseThrow(() -> new ProductNotFoundException("Product with ID " + itemRequest.getProductId() + " not found"));
//
//            Supplier supplier = product.getSupplier();
//            if (supplier == null || !supplierRepository.existsById(supplier.getId())) {
//                throw new SupplierNotFoundException("Supplier not found for product with ID " + product.getId());
//            }
//
//            if (product.getStockQuantity() < itemRequest.getQuantity()) {
//                throw new NotEnoughProductStockException("Not enough stock for product ID " + product.getId());
//            }
//
//            if (itemRequest.getQuantity() <= 0) {
//                throw new NegativeOrderQuantityException("Quantity must be greater than zero for product ID " + product.getId());
//            }
//
//            product.reduceStock(itemRequest.getQuantity());
//            productRepository.save(product);
//
//            OrderItem orderItem = OrderItem.builder()
//                    .product(product)
//                    .supplier(supplier)
//                    .quantity(itemRequest.getQuantity())
//                    .build();
//            items.add(orderItem);
//        }
//
//        Order order = Order.builder()
//                .user(user)
//                .orderItems(items)
//                .status(OrderStatus.PENDING)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        return orderRepository.save(order);
    }

    @Transactional
    public Order createOrderByProductAndSupplierName(MyUser user, List<CreateOrderWithProductSupplierRequest> orderItems) {
       return null;
    }

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public List<Order> getUserOrders(Long userId) {
        logger.info("Fetching orders for user ID: {}", userId);
        return orderRepository.findAllByUserId(userId);
    }

    @Transactional
    public void cancelOrder(Long orderId, MyUser user) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));
//
//        if (!order.getUser().getId().equals(user.getId())) {
//            throw new NotOwnOrderException("User does not own this order");
//        }
//
//        if (order.getStatus() != OrderStatus.PENDING) {
//            throw new OrderCannotBeCancelledException("Order cannot be cancelled, status is: " + order.getStatus());
//        }
//
//        order.setStatus(OrderStatus.CANCELLED);
//        orderRepository.save(order);
//        logger.info("Order with ID {} has been cancelled", orderId);
    }
}