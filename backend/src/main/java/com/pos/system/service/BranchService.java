package com.pos.system.service;

import com.pos.system.entity.Branch;
import com.pos.system.entity.UserBranch;
import com.pos.system.repository.BranchRepository;
import com.pos.system.repository.UserBranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserBranchRepository userBranchRepository;

    @Transactional(readOnly = true)
    public List<Branch> findAll() {
        return branchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Branch> findActive() {
        return branchRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public Branch findById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + id));
    }

    @Transactional
    public Branch create(Branch branch) {
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch update(Long id, Branch update) {
        Branch existing = findById(id);
        existing.setNombre(update.getNombre());
        existing.setDireccion(update.getDireccion());
        existing.setTelefono(update.getTelefono());
        existing.setEmail(update.getEmail());
        existing.setResponsableId(update.getResponsableId());
        existing.setActiva(update.getActiva());
        existing.setTimezone(update.getTimezone());
        existing.setPuntoVentaFiscal(update.getPuntoVentaFiscal());
        return branchRepository.save(existing);
    }

    @Transactional
    public void assignUser(Long userId, Long branchId) {
        if (!userBranchRepository.existsByIdUserIdAndIdBranchId(userId, branchId)) {
            userBranchRepository.save(UserBranch.builder()
                    .id(new UserBranch.UserBranchId(userId, branchId))
                    .activo(true)
                    .build());
        }
    }

    @Transactional
    public void removeUser(Long userId, Long branchId) {
        userBranchRepository.deleteById(new UserBranch.UserBranchId(userId, branchId));
    }

    @Transactional(readOnly = true)
    public List<Long> getUserBranchIds(Long userId) {
        return userBranchRepository.findActiveBranchIdsByUserId(userId);
    }
}
