package com.pos.system.repository;

import com.pos.system.entity.EcommerceSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EcommerceSyncLogRepository extends JpaRepository<EcommerceSyncLog, Long> {

    List<EcommerceSyncLog> findTop50ByConfigIdOrderByTimestampDesc(Long configId);

    long countByConfigIdAndResultadoAndTimestampAfter(Long configId, String resultado, LocalDateTime since);

    long countByConfigIdAndTipoAndTimestampAfter(Long configId, String tipo, LocalDateTime since);
}
