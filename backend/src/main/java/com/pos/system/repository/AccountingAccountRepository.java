package com.pos.system.repository;

import com.pos.system.entity.AccountingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, Long> {

    Optional<AccountingAccount> findByCodigo(String codigo);

    List<AccountingAccount> findByActivaTrueOrderByCodigo();

    List<AccountingAccount> findByCuentaPadreIsNullOrderByCodigo();

    List<AccountingAccount> findByTipoOrderByCodigo(AccountingAccount.Tipo tipo);
}
