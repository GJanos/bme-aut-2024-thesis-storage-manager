package com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto;

import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierCreationResponse {
    String userName;
    String generatedPassword;
    String token;
    Supplier supplier;
}
