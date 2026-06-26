package com.pos.system.repository;

import com.pos.system.entity.EcommerceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcommerceOrderRepository extends JpaRepository<EcommerceOrder, Long> {

    Optional<EcommerceOrder> findByConfigIdAndExternalOrderId(Long configId, String externalOrderId);

    List<EcommerceOrder> findByConfigIdAndStatus(Long configId, String status);

    long countByConfigIdAndStatus(Long configId, String status);
}
