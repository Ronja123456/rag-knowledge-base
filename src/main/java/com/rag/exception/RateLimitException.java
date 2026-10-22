package com.rag.exception;

public class RateLimitException extends RuntimeException {

    public static final String PUBLIC_MESSAGE = "今日咨询次数已达上限，请明天再试或联系人工";

    public RateLimitException() {
        super(PUBLIC_MESSAGE);
    }
}
