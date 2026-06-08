package com.pos.system.dto.response;

import java.util.List;

/**
 * Respuesta del libro de ventas con filas, totales y detección de saltos.
 */
public record SalesBookResponse(
        List<SalesBookRow> filas,
        SalesBookTotals totales,
        List<Long> saltosEncontrados,
        int pagina,
        int tamanioPagina,
        long totalElementos,
        int totalPaginas
) {
    public boolean haySaltos() {
        return saltosEncontrados != null && !saltosEncontrados.isEmpty();
    }
}
