package com.pos.system.repository;

import com.pos.system.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByActivaTrue();
    List<Recipe> findByProductoTerminadoId(Long productoTerminadoId);
    boolean existsByProductoTerminadoIdAndActivaTrue(Long productoTerminadoId);
}
