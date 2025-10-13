package com.tguard.tguard_backend.common.tenant;

import org.springframework.util.StringUtils;

public final class TenantContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(tenantId.trim());
        }
    }

    public static String getTenantId() {
        return CONTEXT.get();
    }

    public static String requireTenantId() {
        String tenantId = CONTEXT.get();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("Tenant id is not bound to the current context");
        }
        return tenantId;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
