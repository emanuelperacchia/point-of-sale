package com.pos.system.service;

import com.pos.system.dto.request.AccountingAccountRequest;
import com.pos.system.dto.response.AccountingAccountResponse;
import com.pos.system.entity.AccountingAccount;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.AccountingAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingAccountService {

    private final AccountingAccountRepository repository;

    @Transactional
    public AccountingAccountResponse create(AccountingAccountRequest request) {
        if (repository.findByCodigo(request.getCodigo()).isPresent()) {
            throw new BadRequestException("Ya existe una cuenta con código " + request.getCodigo());
        }

        AccountingAccount.Tipo tipo;
        try {
            tipo = AccountingAccount.Tipo.valueOf(request.getTipo());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo inválido: " + request.getTipo());
        }

        AccountingAccount cuentaPadre = null;
        if (request.getCuentaPadreId() != null) {
            cuentaPadre = repository.findById(request.getCuentaPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cuenta padre no encontrada"));
        }

        Integer nivel = request.getNivel();
        if (nivel == null) {
            nivel = cuentaPadre != null ? cuentaPadre.getNivel() + 1 : 1;
        }

        AccountingAccount account = AccountingAccount.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .tipo(tipo)
                .cuentaPadre(cuentaPadre)
                .nivel(nivel)
                .activa(request.getActiva() != null ? request.getActiva() : true)
                .build();

        account = repository.save(account);
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountingAccountResponse> listActive() {
        return repository.findByActivaTrueOrderByCodigo().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountingAccountResponse getById(Long id) {
        AccountingAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta contable no encontrada"));
        return mapToResponse(account);
    }

    @Transactional
    public AccountingAccountResponse update(Long id, AccountingAccountRequest request) {
        AccountingAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta contable no encontrada"));

        if (request.getNombre() != null) account.setNombre(request.getNombre());
        if (request.getActiva() != null) account.setActiva(request.getActiva());

        account = repository.save(account);
        return mapToResponse(account);
    }

    private AccountingAccountResponse mapToResponse(AccountingAccount account) {
        return AccountingAccountResponse.builder()
                .id(account.getId())
                .codigo(account.getCodigo())
                .nombre(account.getNombre())
                .tipo(account.getTipo().name())
                .cuentaPadreId(account.getCuentaPadre() != null ? account.getCuentaPadre().getId() : null)
                .cuentaPadreNombre(account.getCuentaPadre() != null ? account.getCuentaPadre().getNombre() : null)
                .nivel(account.getNivel())
                .activa(account.getActiva())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
