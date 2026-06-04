package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResponse {

    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private Integer stock;
    private String categoryName;
    private boolean active;
}
