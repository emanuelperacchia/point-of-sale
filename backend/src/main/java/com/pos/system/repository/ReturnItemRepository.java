package com.pos.system.repository;

import com.pos.system.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {
    List<ReturnItem> findByReturnId(Long returnId);
}
