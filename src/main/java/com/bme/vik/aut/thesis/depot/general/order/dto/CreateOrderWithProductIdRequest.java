package com.bme.vik.aut.thesis.depot.general.order.dto;

import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderWithProductIdRequest {
    private Long productId;
    private int quantity;
}
