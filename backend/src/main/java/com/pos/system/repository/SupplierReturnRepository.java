package com.pos.system.repository;

import com.pos.system.entity.Supplier;
import com.pos.system.entity.SupplierReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para devoluciones a proveedores.
 */
@Repository
public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, Long> {

    Optional<SupplierReturn> findByReturnNumber(String returnNumber);

    List<SupplierReturn> findBySupplier(Supplier supplier);

    List<SupplierReturn> findByStatus(SupplierReturn.ReturnStatus status);

    boolean existsByReturnNumber(String returnNumber);

    /**
     * Obtiene el siguiente número correlativo de devolución para un prefijo dado.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(s.returnNumber, 4) AS integer)), 0) "
            + "FROM SupplierReturn s WHERE s.returnNumber LIKE :prefix")
    Integer getNextReturnNumber(@Param("prefix") String prefix);

    /**
     * Obtiene el siguiente número correlativo de nota de crédito para un prefijo dado.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(s.creditNoteNumber, 4) AS integer)), 0) "
            + "FROM SupplierReturn s WHERE s.creditNoteNumber LIKE :prefix")
    Integer getNextCreditNoteNumber(@Param("prefix") String prefix);
}
