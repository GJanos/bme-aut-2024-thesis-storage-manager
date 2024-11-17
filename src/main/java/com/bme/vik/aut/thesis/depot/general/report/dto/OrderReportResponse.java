package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.order.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrderReportResponse {
    private int numOfOrders;
    private Map<OrderStatus, Long> orderStats;
    private List<UserOrder> userOrders;
}
