package com.pos.system.config;

/**
 * ThreadLocal que almacena el branchId del request actual.
 * Similar a SecurityContextHolder, permite que los servicios
 * filtren automáticamente por sucursal sin pasar el parámetro
 * explícitamente en cada método.
 *
 * Si branchId es null, significa que el usuario puede ver
 * TODAS las sucursales (rol ADMIN).
 */
public class BranchContextHolder {

    private static final ThreadLocal<Long> BRANCH_ID = new ThreadLocal<>();

    public static void setBranchId(Long branchId) {
        BRANCH_ID.set(branchId);
    }

    public static Long getBranchId() {
        return BRANCH_ID.get();
    }

    public static void clear() {
        BRANCH_ID.remove();
    }
}
