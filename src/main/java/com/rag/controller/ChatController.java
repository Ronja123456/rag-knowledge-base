package com.rag.controller;


import com.rag.common.Result;
import com.rag.service.AgentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    // 👈 核心修改：加上 produces 指定为 SSE 流
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam("msg") String message,
                           @RequestParam(value = "sessionId", defaultValue = "default_user") String sessionId) {
        return agentService.streamChat(message, sessionId);
    }
}