package com.pos.system.repository;

import com.pos.system.entity.SystemBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemBackupRepository extends JpaRepository<SystemBackup, Long> {

    List<SystemBackup> findAllByOrderByCreatedAtDesc();
}
