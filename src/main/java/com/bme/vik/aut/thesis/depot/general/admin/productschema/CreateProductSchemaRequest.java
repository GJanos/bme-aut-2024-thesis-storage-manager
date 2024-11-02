package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductSchemaRequest {
    private String name;
    private Integer storageSpaceNeeded;
    private List<Long> categoryIDs;
}