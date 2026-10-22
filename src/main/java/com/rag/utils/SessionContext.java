package com.rag.utils;

public class SessionContext {
    private static final ThreadLocal<String> SESSION_HOLDER = new ThreadLocal<>();

    public static void setSessionId(String sessionId) {
        SESSION_HOLDER.set(sessionId);
    }

    public static String getSessionId() {
        return SESSION_HOLDER.get();
    }

    public static void clear() {
        SESSION_HOLDER.remove();
    }
}