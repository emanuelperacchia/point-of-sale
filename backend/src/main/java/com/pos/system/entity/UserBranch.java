package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "user_branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranch {

    @EmbeddedId
    private UserBranchId id;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBranchId implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "branch_id")
        private Long branchId;
    }
}
