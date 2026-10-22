package com.rag.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface MemoryService {
    void saveHistory(String sessionId, String role, String content);
    List<Message> getHistory(String sessionId);
    void saveSlot(String sessionId, String shopId, String shopName);
    String getCurrentShopName(String sessionId);
}