package com.pos.system.repository;

import com.pos.system.entity.ApiKeyUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiKeyUsageLogRepository extends JpaRepository<ApiKeyUsageLog, Long> {

    long countByApiKeyIdAndTimestampAfter(Long apiKeyId, LocalDateTime since);

    @Query("SELECT COUNT(l) FROM ApiKeyUsageLog l WHERE l.apiKeyId = :keyId AND l.statusCode >= 400")
    long countErrorsByApiKeyId(@Param("keyId") Long keyId);

    @Query("SELECT COUNT(l) FROM ApiKeyUsageLog l WHERE l.apiKeyId = :keyId " +
           "AND l.timestamp >= :since AND l.statusCode >= 400")
    long countErrorsSince(@Param("keyId") Long keyId, @Param("since") LocalDateTime since);

    List<ApiKeyUsageLog> findTop100ByApiKeyIdOrderByTimestampDesc(Long apiKeyId);
}
