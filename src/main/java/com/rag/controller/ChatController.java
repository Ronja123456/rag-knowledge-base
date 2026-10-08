package com.rag.controller;

import com.rag.utils.DeepSeekClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private DeepSeekClient deepSeekClient;
    @PostConstruct
    public void init() {
        System.out.println("========== ChatController 被创建了 ==========");
    }

    @GetMapping("/test")
    public Map<String, Object> test(@RequestParam String msg) {
        String answer = deepSeekClient.chat(msg);
        Map<String, Object> result = new HashMap<>();
        result.put("question", msg);
        result.put("answer", answer);
        return result;
    }

}