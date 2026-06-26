package com.pos.system.service;

import com.pos.system.dto.request.BranchPriceRequest;
import com.pos.system.dto.response.BranchPriceResponse;

import java.util.List;

public interface BranchPriceService {

    BranchPriceResponse create(Long branchId, BranchPriceRequest request, Long userId);

    BranchPriceResponse update(Long branchId, Long priceId, BranchPriceRequest request);

    void delete(Long branchId, Long priceId);

    List<BranchPriceResponse> getByBranch(Long branchId);

    BranchPriceResponse getById(Long branchId, Long priceId);

    /**
     * Desactiva todos los precios locales de la sucursal y vuelve al precio global.
     */
    void syncGlobal(Long branchId);
}
