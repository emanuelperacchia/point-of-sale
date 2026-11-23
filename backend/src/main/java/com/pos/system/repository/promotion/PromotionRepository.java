package com.pos.system.repository.promotion;

import com.pos.system.entity.promotion.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findByCouponCode(String couponCode);

    List<Promotion> findByBusinessId(Long businessId);

    List<Promotion> findByActiveTrue();

    List<Promotion> findByActiveTrueAndExpiresAtAfter(LocalDateTime now);

    List<Promotion> findByBusinessIdAndActiveTrue(Long businessId);

    boolean existsByCouponCode(String couponCode);
}
