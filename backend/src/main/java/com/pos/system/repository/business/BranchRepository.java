package com.pos.system.repository.business;

import com.pos.system.entity.business.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

    Optional<Branch> findByBranchName(String branchName);

    Optional<Branch> findBySlug(String slug);

    List<Branch> findByActiveTrue();

    List<Branch> findBySubscriptionStatus(String subscriptionStatus);

    boolean existsByBranchName(String branchName);

    boolean existsBySlug(String slug);
}
