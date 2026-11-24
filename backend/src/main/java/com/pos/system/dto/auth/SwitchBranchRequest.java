package com.pos.system.dto.auth;

public class SwitchBranchRequest {
    private Long branchId;

    public SwitchBranchRequest() {}

    public SwitchBranchRequest(Long branchId) {
        this.branchId = branchId;
    }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
}
