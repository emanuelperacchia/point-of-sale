package com.pos.system.service.business;

import com.pos.system.dto.business.BusinessCreateRequest;
import com.pos.system.dto.business.BusinessResponse;
import com.pos.system.entity.business.Branch;
import com.pos.system.entity.business.Branch.BusinessType;
import com.pos.system.entity.business.Branch.BusinessStatus;
import com.pos.system.entity.business.Branch.SubscriptionPlan;
import com.pos.system.entity.business.Branch.SubscriptionStatus;
import com.pos.system.repository.business.BranchRepository;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BusinessService {

    @Autowired
    private BranchRepository branchRepository;

    public BusinessResponse createBusiness(BusinessCreateRequest request) {
        if (branchRepository.existsByBranchName(request.getBusinessName())) {
            throw new BusinessException("Ya existe un negocio con este nombre");
        }

        if (branchRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException("Ya existe un negocio con este slug");
        }

        Branch business = Branch.builder()
                .branchName(request.getBusinessName())
                .slug(request.getSlug())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .taxId(request.getTaxId())
                .businessType(BusinessType.valueOf(request.getBusinessType()))
                .subscriptionPlan(SubscriptionPlan.valueOf(request.getSubscriptionPlan()))
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .monthlyFee(request.getMonthlyFee())
                .active(true)
                .termsAccepted(request.getTermsAccepted())
                .build();

        Branch saved = branchRepository.save(business);
        return mapToResponse(saved);
    }

    public BusinessResponse getBusinessById(Long id) {
        Branch business = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));
        return mapToResponse(business);
    }

    public BusinessResponse getBusinessBySlug(String slug) {
        Branch business = branchRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con slug: " + slug));
        return mapToResponse(business);
    }

    public List<BusinessResponse> getAllBusinesses() {
        return branchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<BusinessResponse> getAllBusinesses(Pageable pageable) {
        return branchRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public List<BusinessResponse> getActiveBusinesses() {
        return branchRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BusinessResponse updateBusiness(Long id, BusinessCreateRequest request) {
        Branch business = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        business.setBranchName(request.getBusinessName());
        business.setSlug(request.getSlug());
        business.setAddress(request.getAddress());
        business.setPhone(request.getPhone());
        business.setEmail(request.getEmail());
        business.setTaxId(request.getTaxId());
        business.setBusinessType(BusinessType.valueOf(request.getBusinessType()));
        business.setSubscriptionPlan(SubscriptionPlan.valueOf(request.getSubscriptionPlan()));
        business.setMonthlyFee(request.getMonthlyFee());
        business.setTermsAccepted(request.getTermsAccepted());

        Branch saved = branchRepository.save(business);
        return mapToResponse(saved);
    }

    public BusinessResponse deleteBusiness(Long id) {
        Branch business = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));
        business.setActive(false);
        Branch saved = branchRepository.save(business);
        return mapToResponse(saved);
    }

    public BusinessResponse updateSubscription(Long businessId, SubscriptionPlan plan, SubscriptionStatus status) {
        Branch business = branchRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + businessId));

        business.setSubscriptionPlan(plan);
        business.setSubscriptionStatus(status);
        if (status == SubscriptionStatus.ACTIVE) {
            business.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30));
        }

        Branch saved = branchRepository.save(business);
        return mapToResponse(saved);
    }

    private BusinessResponse mapToResponse(Branch business) {
        BusinessResponse response = new BusinessResponse();
        response.setId(business.getId());
        response.setBranchName(business.getBranchName());
        response.setSlug(business.getSlug());
        response.setAddress(business.getAddress());
        response.setPhone(business.getPhone());
        response.setEmail(business.getEmail());
        response.setTaxId(business.getTaxId());
        response.setBusinessType(business.getBusinessType().name());
        response.setSubscriptionPlan(business.getSubscriptionPlan().name());
        response.setSubscriptionStatus(business.getSubscriptionStatus().name());
        response.setMonthlyFee(business.getMonthlyFee());
        response.setActive(business.isActive());
        response.setTermsAccepted(business.isTermsAccepted());
        response.setSubscriptionExpiresAt(business.getSubscriptionExpiresAt());
        response.setTrialEndsAt(business.getTrialEndsAt());
        response.setCreatedAt(business.getCreatedAt());
        response.setUpdatedAt(business.getUpdatedAt());
        return response;
    }
}
