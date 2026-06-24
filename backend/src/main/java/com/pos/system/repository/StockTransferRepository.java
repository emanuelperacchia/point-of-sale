package com.pos.system.repository;

import com.pos.system.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    List<StockTransfer> findBySucursalOrigenIdOrderByFechaSolicitudDesc(Long sucursalOrigenId);

    List<StockTransfer> findBySucursalDestinoIdOrderByFechaSolicitudDesc(Long sucursalDestinoId);

    List<StockTransfer> findByEstadoOrderByFechaSolicitudDesc(StockTransfer.Estado estado);

    List<StockTransfer> findBySucursalOrigenIdAndSucursalDestinoIdOrderByFechaSolicitudDesc(
            Long sucursalOrigenId, Long sucursalDestinoId);

    List<StockTransfer> findByFechaSolicitudBetweenOrderByFechaSolicitudDesc(
            LocalDateTime desde, LocalDateTime hasta);
}
