package com.pos.system.controller.business;

import com.pos.system.dto.business.BusinessCreateRequest;
import com.pos.system.dto.business.BusinessResponse;
import com.pos.system.service.business.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    @Autowired
    private BusinessService businessService;

    @PostMapping
    public ResponseEntity<BusinessResponse> createBusiness(@RequestBody BusinessCreateRequest request) {
        BusinessResponse business = businessService.createBusiness(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(business);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> getBusinessById(@PathVariable Long id) {
        BusinessResponse business = businessService.getBusinessById(id);
        return ResponseEntity.ok(business);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<BusinessResponse> getBusinessBySlug(@PathVariable String slug) {
        BusinessResponse business = businessService.getBusinessBySlug(slug);
        return ResponseEntity.ok(business);
    }

    @GetMapping
    public ResponseEntity<List<BusinessResponse>> getAllBusinesses() {
        List<BusinessResponse> businesses = businessService.getAllBusinesses();
        return ResponseEntity.ok(businesses);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BusinessResponse>> getActiveBusinesses() {
        List<BusinessResponse> businesses = businessService.getActiveBusinesses();
        return ResponseEntity.ok(businesses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> updateBusiness(@PathVariable Long id, @RequestBody BusinessCreateRequest request) {
        BusinessResponse business = businessService.updateBusiness(id, request);
        return ResponseEntity.ok(business);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        businessService.deleteBusiness(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/subscription")
    public ResponseEntity<BusinessResponse> updateSubscription(
            @PathVariable Long id,
            @RequestParam String plan,
            @RequestParam String status) {
        BusinessResponse business = businessService.updateSubscription(
                id,
                com.pos.system.entity.business.Branch.SubscriptionPlan.valueOf(plan),
                com.pos.system.entity.business.Branch.SubscriptionStatus.valueOf(status)
        );
        return ResponseEntity.ok(business);
    }
}
