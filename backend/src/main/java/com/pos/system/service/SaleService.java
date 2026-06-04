package com.pos.system.service;

import com.pos.system.dto.request.CartValidationRequest;
import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.response.CartValidationResponse;
import com.pos.system.dto.response.SaleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SaleService {

    SaleResponse processSale(SaleRequest request, Long userId);

    SaleResponse getById(Long id);

    Page<SaleResponse> getSalesByUser(Long userId, Pageable pageable);

    CartValidationResponse validateCart(CartValidationRequest request);
}
