package com.pos.system.repository;

import com.pos.system.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByActivoTrue();

    @Query("SELECT e FROM Employee e WHERE " +
           "(:departamento IS NULL OR e.departamento = :departamento) AND " +
           "(:cargo IS NULL OR e.cargo = :cargo) AND " +
           "(:sucursalId IS NULL OR e.sucursalId = :sucursalId) AND " +
           "(:activo IS NULL OR e.activo = :activo)")
    List<Employee> findByFilters(@Param("departamento") String departamento,
                                 @Param("cargo") String cargo,
                                 @Param("sucursalId") Long sucursalId,
                                 @Param("activo") Boolean activo);

    boolean existsByDni(String dni);

    boolean existsByUserId(Long userId);
}
