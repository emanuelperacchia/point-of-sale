package com.pos.system.repository;

import com.pos.system.entity.AccountingJournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingJournalEntryRepository extends JpaRepository<AccountingJournalEntry, Long> {

    List<AccountingJournalEntry> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

    @Query("SELECT e FROM AccountingJournalEntry e WHERE e.referenciaId = :refId AND e.referenciaType = :refType")
    Optional<AccountingJournalEntry> findByReferencia(@Param("refId") Long refId, @Param("refType") String refType);

    List<AccountingJournalEntry> findByWebhookEnviadoFalse();

    boolean existsByReferenciaIdAndReferenciaType(Long referenciaId, String referenciaType);
}
