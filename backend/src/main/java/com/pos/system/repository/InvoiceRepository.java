package com.pos.system.repository;

import com.pos.system.entity.InvoiceDocument;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceDocument, Long> {

    Optional<InvoiceDocument> findBySaleId(Long saleId);

    List<InvoiceDocument> findByEstado(InvoiceStatus estado);

    List<InvoiceDocument> findByTipoComprobanteAndNumeroBetween(
            TipoComprobante tipo, Long desde, Long hasta);

    List<InvoiceDocument> findByEstadoAndCreatedAtBefore(InvoiceStatus estado, LocalDateTime before);

    /**
     * Obtiene el próximo valor de la secuencia PostgreSQL según el tipo de comprobante.
     * <p>
     * Reemplaza el approach anterior de SELECT MAX + 1 que sufría race conditions.
     * Las sequences son creadas por V9: seq_boleta, seq_factura_a, etc.
     * </p>
     */
    @Query(value = """
            SELECT CASE :tipo
                WHEN 'BOLETA' THEN NEXTVAL('seq_boleta')
                WHEN 'FACTURA_A' THEN NEXTVAL('seq_factura_a')
                WHEN 'FACTURA_B' THEN NEXTVAL('seq_factura_b')
                WHEN 'FACTURA_C' THEN NEXTVAL('seq_factura_c')
                WHEN 'NOTA_CREDITO' THEN NEXTVAL('seq_nota_credito')
                WHEN 'NOTA_DEBITO' THEN NEXTVAL('seq_nota_debito')
                ELSE NEXTVAL('seq_boleta')
            END
            """, nativeQuery = true)
    Long nextSequenceValue(@Param("tipo") String tipoComprobante);

    @Query("SELECT i FROM InvoiceDocument i WHERE " +
           "(:tipo IS NULL OR i.tipoComprobante = :tipo) AND " +
           "(:estado IS NULL OR i.estado = :estado) AND " +
           "(:desde IS NULL OR i.createdAt >= :desde) AND " +
           "(:hasta IS NULL OR i.createdAt <= :hasta) AND " +
           "(:saleId IS NULL OR i.saleId = :saleId)")
    List<InvoiceDocument> findByFilters(
            @Param("tipo") TipoComprobante tipo,
            @Param("estado") InvoiceStatus estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("saleId") Long saleId);
}
