package com.pos.system.repository;

import com.pos.system.entity.AccountingEntryTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingEntryTemplateRepository extends JpaRepository<AccountingEntryTemplate, Long> {

    @EntityGraph(attributePaths = {"lineas", "lineas.cuenta"})
    Optional<AccountingEntryTemplate> findByEventoOrigenAndActivoTrue(AccountingEntryTemplate.EventoOrigen eventoOrigen);
}
