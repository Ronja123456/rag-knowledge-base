package com.rag.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> rateLimit(RateLimitException e) {
        return body(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> aiUnavailable(AiServiceException e) {
        log.warn("AI 服务降级: {}", e.getCause() == null ? e.getMessage() : e.getCause().getClass().getSimpleName());
        return body(HttpStatus.SERVICE_UNAVAILABLE, AiServiceException.PUBLIC_MESSAGE);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        String message = "请求无效，请检查知识库与问题内容";
        if (e instanceof IllegalArgumentException && e.getMessage() != null && !e.getMessage().isBlank()) {
            message = e.getMessage();
        }
        return body(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> fallback(Exception e) {
        log.error("未处理异常，已对用户降级", e);
        return body(HttpStatus.SERVICE_UNAVAILABLE, AiServiceException.PUBLIC_MESSAGE);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", status.value());
        payload.put("message", message);
        return ResponseEntity.status(status).body(payload);
    }
}
