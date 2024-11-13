package com.bme.vik.aut.thesis.depot.general.info.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private List<OrderItemResponse> orderItems;
    private String status;
    private LocalDateTime createdAt;
}