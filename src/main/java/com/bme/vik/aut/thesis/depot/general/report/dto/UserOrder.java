package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.order.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserOrder {
    private Long orderID;
    private Long userID;
    private OrderStatus orderStatus;
    private List<ProductState> products;
    private LocalDateTime createdAt;
}
