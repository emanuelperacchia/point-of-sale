package com.pos.system.dto.request;

import lombok.Data;

import java.util.List;

/**
 * Request para iniciar una devolución.
 */
@Data
public class CreateReturnRequest {
    private Long saleId;
    private String motivo;
    private List<ReturnItemRequest> items;

    @Data
    public static class ReturnItemRequest {
        private Long saleItemId;
        private Integer cantidad;
    }
}
