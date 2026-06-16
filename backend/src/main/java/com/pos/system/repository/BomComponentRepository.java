package com.pos.system.repository;

import com.pos.system.entity.BomComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BomComponentRepository extends JpaRepository<BomComponent, Long> {
    List<BomComponent> findByRecipeId(Long recipeId);
    void deleteByRecipeId(Long recipeId);
}
