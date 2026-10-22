package com.rag.exception;

public class AiServiceException extends RuntimeException {

    public static final String PUBLIC_MESSAGE = "当前咨询较多，请稍后再试或联系人工";

    public AiServiceException() {
        super(PUBLIC_MESSAGE);
    }

    public AiServiceException(Throwable cause) {
        super(PUBLIC_MESSAGE, cause);
    }
    public AiServiceException(String message) {
        super(message);
    }
}
