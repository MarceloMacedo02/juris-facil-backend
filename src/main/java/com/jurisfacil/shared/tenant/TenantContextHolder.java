package com.jurisfacil.shared.tenant;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CTX = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CTX.set(context);
    }

    public static TenantContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}
