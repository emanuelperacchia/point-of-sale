package com.pos.system.repository;

import com.pos.system.entity.DigitalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DigitalCertificateRepository extends JpaRepository<DigitalCertificate, Long> {
    Optional<DigitalCertificate> findByActiveTrue();
}
