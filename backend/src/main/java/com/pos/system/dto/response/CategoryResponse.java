package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private Integer productCount;
    private LocalDateTime createdAt;
}
