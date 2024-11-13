package com.bme.vik.aut.thesis.depot.general.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserId(Long userId);

    List<Order> findAllByStatus(OrderStatus status);

    Optional<Order> findByIdAndStatus(Long id, OrderStatus status);
}
