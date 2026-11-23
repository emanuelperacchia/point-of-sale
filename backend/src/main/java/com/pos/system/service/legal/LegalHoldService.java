package com.pos.system.service.legal;

import com.pos.system.dto.legal.LegalHoldRequest;
import com.pos.system.dto.legal.LegalHoldResponse;
import com.pos.system.entity.business.Branch;
import com.pos.system.entity.legal.LegalHold;
import com.pos.system.entity.system.User;
import com.pos.system.repository.business.BranchRepository;
import com.pos.system.repository.legal.LegalHoldRepository;
import com.pos.system.repository.system.UserRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LegalHoldService {

    @Autowired
    private LegalHoldRepository legalHoldRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    public LegalHoldResponse createLegalHold(Long businessId, Long userId, LegalHoldRequest request) {
        Branch business = branchRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado con ID: " + businessId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        LegalHold legalHold = LegalHold.builder()
                .business(business)
                .user(user)
                .holdType(request.getHoldType())
                .reason(request.getReason())
                .legalBasis(request.getLegalBasis())
                .active(true)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveUntil(request.getEffectiveUntil())
                .dataDeletionDate(request.getDataDeletionDate())
                .legalRetentionDays(request.getLegalRetentionDays() != null ? request.getLegalRetentionDays() : 5)
                .build();

        LegalHold saved = legalHoldRepository.save(legalHold);
        return mapToResponse(saved);
    }

    public LegalHoldResponse getLegalHoldById(Long id) {
        LegalHold legalHold = legalHoldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo legal no encontrado con ID: " + id));
        return mapToResponse(legalHold);
    }

    public List<LegalHoldResponse> getLegalHoldsByBusiness(Long businessId) {
        return legalHoldRepository.findByBusinessId(businessId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LegalHoldResponse> getActiveLegalHolds() {
        return legalHoldRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LegalHoldResponse updateLegalHold(Long id, LegalHoldRequest request) {
        LegalHold legalHold = legalHoldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo legal no encontrado con ID: " + id));

        legalHold.setHoldType(request.getHoldType());
        legalHold.setReason(request.getReason());
        legalHold.setLegalBasis(request.getLegalBasis());
        legalHold.setActive(request.isActive());
        legalHold.setEffectiveFrom(request.getEffectiveFrom());
        legalHold.setEffectiveUntil(request.getEffectiveUntil());
        legalHold.setDataDeletionDate(request.getDataDeletionDate());
        legalHold.setLegalRetentionDays(request.getLegalRetentionDays());

        LegalHold saved = legalHoldRepository.save(legalHold);
        return mapToResponse(saved);
    }

    public LegalHoldResponse resolveLegalHold(Long id) {
        LegalHold legalHold = legalHoldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo legal no encontrado con ID: " + id));

        legalHold.setResolvedAt(LocalDateTime.now());
        LegalHold saved = legalHoldRepository.save(legalHold);
        return mapToResponse(saved);
    }

    private LegalHoldResponse mapToResponse(LegalHold legalHold) {
        LegalHoldResponse response = new LegalHoldResponse();
        response.setId(legalHold.getId());
        response.setBusinessId(legalHold.getBusiness().getId());
        response.setBusinessName(legalHold.getBusiness().getBranchName());
        response.setUserId(legalHold.getUser().getId());
        response.setUserName(legalHold.getUser().getFullName());
        response.setHoldType(legalHold.getHoldType());
        response.setReason(legalHold.getReason());
        response.setLegalBasis(legalHold.getLegalBasis());
        response.setActive(legalHold.isActive());
        response.setEffectiveFrom(legalHold.getEffectiveFrom());
        response.setEffectiveUntil(legalHold.getEffectiveUntil());
        response.setDataDeletionDate(legalHold.getDataDeletionDate());
        response.setLegalRetentionDays(legalHold.getLegalRetentionDays());
        response.setCreatedAt(legalHold.getCreatedAt());
        response.setResolvedAt(legalHold.getResolvedAt());
        response.setResolvedBy(legalHold.getResolvedBy());
        return response;
    }
}
