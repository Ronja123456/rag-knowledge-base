package com.rag.service;


import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentService {
    // 原来的 Result<String> chat 改为返回 SseEmitter
    SseEmitter streamChat(String message, String sessionId);
}