package com.rag.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String from(HttpServletRequest request) {
        String forwarded = firstHeader(request, "X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = firstHeader(request, "X-Real-IP");
        if (realIp != null) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private static String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }
}
