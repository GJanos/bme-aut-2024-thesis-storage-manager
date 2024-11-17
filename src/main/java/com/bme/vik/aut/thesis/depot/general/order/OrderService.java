package com.bme.vik.aut.thesis.depot.general.order;

import com.bme.vik.aut.thesis.depot.exception.order.*;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.order.dto.COWProductId;
import com.bme.vik.aut.thesis.depot.general.order.dto.COWProductName;
import com.bme.vik.aut.thesis.depot.general.order.dto.COWProductSupplierName;
import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductService;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierService;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final ProductSchemaService productSchemaService;


    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));
    }

    @Transactional
    public Order createOrder(MyUser user, List<CreateOrderRequest> orderItems) {
        logger.info("Creating new order for user ID: {}", user.getId());

        // Step 1: Calculate total stock requirements for each product
        Map<Long, Integer> requiredStock = new HashMap<>(); // Map of product schema ID to required quantity
        for (CreateOrderRequest productRequest : orderItems) {
            Long productSchemaId;
            int quantity;

            if (productRequest instanceof COWProductName productNameRequest) {
                productSchemaId = productSchemaService.getProductSchemaByName(productNameRequest.getProductName()).getId();
                quantity = productNameRequest.getQuantity();

            } else if (productRequest instanceof COWProductSupplierName productSupplierNameRequest) {
                productSchemaId = productSchemaService.getProductSchemaByName(productSupplierNameRequest.getProductName()).getId();
                quantity = productSupplierNameRequest.getQuantity();

            } else if (productRequest instanceof COWProductId productIdRequest) {
                Product product = productService.getProductById(productIdRequest.getProductId());
                productSchemaId = product.getSchema().getId();
                quantity = productIdRequest.getQuantity();

            } else {
                throw new InvalidCreateOrderRequestException("Unsupported CreateOrderRequest derived type");
            }

            requiredStock.merge(productSchemaId, quantity, Integer::sum);
        }

        // Step 2: Validate stock availability for each product schema
        for (Map.Entry<Long, Integer> entry : requiredStock.entrySet()) {
            Long productSchemaId = entry.getKey();
            int requiredQuantity = entry.getValue();
            int availableStock = inventoryService.getCurrentStockBySchemaId(productSchemaId);

            if (availableStock < requiredQuantity) {
                throw new TooLargeOrderException("Not enough stock available for product schema ID " + productSchemaId +
                        ". Required: " + requiredQuantity + ", Available: " + availableStock);
            }
        }

        // Step 3: Reserve products
        List<Product> items = new ArrayList<>();
        for (CreateOrderRequest productRequest : orderItems) {
            List<Product> products;

            if (productRequest instanceof COWProductName productNameRequest) {
                products = prepOrdProdByProdName(productNameRequest);

            } else if (productRequest instanceof COWProductSupplierName productSupplierNameRequest) {
                products = prepOrdProdByProdSupplName(productSupplierNameRequest);

            } else if (productRequest instanceof COWProductId productIdRequest) {
                products = prepOrdProdByProdId(productIdRequest);

            } else {
                throw new InvalidCreateOrderRequestException("Unsupported CreateOrderRequest derived type");
            }
            assert products != null;
            items.addAll(products);
        }

        // Step 4: Create the order with reserved products
        Order order = Order.builder()
                .userId(user.getId())
                .orderItems(items)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return orderRepository.save(order);
    }


    @Transactional
    public void cancelOrder(Long orderId, MyUser user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        boolean isAdmin = user.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !order.getUserId().equals(user.getId())) {
            throw new NotOwnOrderException("User does not own this order with ID: " + orderId);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new NonCancellableOrderException("Order with ID: " + orderId + "  cannot be cancelled, status is: " + order.getStatus());
        }

        inventoryService.freeProducts(order.getOrderItems());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        logger.info("Order with ID {} has been cancelled", orderId);
    }

    public List<Order> getAllPendingOrders() {
        return orderRepository.findAllByStatus(OrderStatus.PENDING);
    }

    public Order getPendingOrderById(Long id) {
        return orderRepository.findByIdAndStatus(id, OrderStatus.PENDING)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + id + " not found or not in pending state"));
    }

    @Transactional
    public Order approvePendingOrder(Long id) {
        Order order = getPendingOrderById(id);
        order.setStatus(OrderStatus.COMPLETED);

        inventoryService.removeCompletedOrderProducts(order.getOrderItems());
        return orderRepository.save(order);
    }

    private List<Product> prepOrdProdByProdName(COWProductName productRequest) {
        logger.info("Processing order by product name: {}", productRequest.getProductName());

        // Validate quantity is positive
        inventoryService.validatePositiveQuantity(productRequest.getQuantity());

        // Find the product schema by name
        ProductSchema schema = productSchemaService.getProductSchemaByName(productRequest.getProductName());

        // Retrieve all inventories that have current stock > 0 for this schema
        List<Inventory> inventoriesWithStock = inventoryService.getInventoriesWithStockForSchema(schema.getId());

        // Sort inventories by soonest expiry date of the available products in each
        inventoriesWithStock.sort((inventory1, inventory2) -> {
            LocalDateTime earliestExpiryInInventory1 = inventoryService.getSoonestExpiryProduct(inventory1, schema).getExpiresAt();
            LocalDateTime earliestExpiryInInventory2 = inventoryService.getSoonestExpiryProduct(inventory2, schema).getExpiresAt();
            return earliestExpiryInInventory1.compareTo(earliestExpiryInInventory2);
        });

        // Reserve products across inventories
        int remainingQuantity = productRequest.getQuantity();
        List<Product> reservedProducts = new ArrayList<>();

        for (Inventory inventory : inventoriesWithStock) {
            if (remainingQuantity == 0) break;

            // Reserve the required quantity, or what is available from the current inventory
            List<Product> reservedFromInventory = inventoryService.reserveProdByProdName(inventory, schema, remainingQuantity);
            reservedProducts.addAll(reservedFromInventory);
            remainingQuantity -= reservedFromInventory.size();

            logger.info("Reserved {} products from inventory ID: {}, remaining quantity to reserve: {}",
                    reservedFromInventory.size(), inventory.getId(), remainingQuantity);
        }

        logger.info("Total products reserved: {}", reservedProducts.size());
        return reservedProducts;
    }


    private List<Product> prepOrdProdByProdSupplName(COWProductSupplierName productRequest) {
        System.out.println("Processing order by product and supplier name: " + productRequest.getProductName() + ", Supplier: " + productRequest.getSupplierName());

        inventoryService.validatePositiveQuantity(productRequest.getQuantity());

        Supplier supplier = supplierService.getSupplierByName(productRequest.getSupplierName());

        ProductSchema schema = productSchemaService.getProductSchemaByName(productRequest.getProductName());

        return inventoryService.reserveProdByProdSupplName(supplier.getInventory(), schema, productRequest.getQuantity());
    }

    private List<Product> prepOrdProdByProdId(COWProductId productRequest) {
        System.out.println("Processing order by product ID: " + productRequest.getProductId());

        Product product = productService.getProductById(productRequest.getProductId());

        if (!productService.isProductAvailable(product)) {
            throw new ProductAlreadyReservedException("Product with ID " + product.getId() + " is already reserved");
        }

        Inventory inventory = inventoryService.getByProductId(product.getId());
        inventoryService.reserveOneProduct(inventory, product);
        logger.info("Product with ID {} reserved successfully.", product.getId());

        return new ArrayList<>(List.of(product));
    }
}