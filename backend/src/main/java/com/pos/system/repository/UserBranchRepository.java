package com.pos.system.repository;

import com.pos.system.entity.UserBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBranchRepository extends JpaRepository<UserBranch, UserBranch.UserBranchId> {

    List<UserBranch> findByIdUserId(Long userId);

    Optional<UserBranch> findByIdUserIdAndIdBranchId(Long userId, Long branchId);

    @Query("SELECT ub.id.branchId FROM UserBranch ub WHERE ub.id.userId = :userId AND ub.activo = true")
    List<Long> findActiveBranchIdsByUserId(@Param("userId") Long userId);

    boolean existsByIdUserIdAndIdBranchId(Long userId, Long branchId);
}
