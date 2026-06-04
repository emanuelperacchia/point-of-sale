package com.pos.system.repository;

import com.pos.system.entity.ReturnStatus;
import com.pos.system.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    List<SaleReturn> findBySaleIdOrderByCreatedAtDesc(Long saleId);

    List<SaleReturn> findByEstado(ReturnStatus estado);
}
