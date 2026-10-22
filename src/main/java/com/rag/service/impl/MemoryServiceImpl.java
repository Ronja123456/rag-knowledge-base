package com.rag.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rag.service.MemoryService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MemoryServiceImpl implements MemoryService {

    private final StringRedisTemplate redisTemplate;

    public MemoryServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveHistory(String sessionId, String role, String content) {
        String key = "chat:memory:v3:" + sessionId;
        JSONObject json = JSONUtil.createObj();
        json.set("role", role);
        json.set("content", content);
        redisTemplate.opsForList().rightPush(key, json.toString());
        redisTemplate.opsForList().trim(key, -10, -1);
    }

    @Override
    public List<Message> getHistory(String sessionId) {
        String key = "chat:memory:v3:" + sessionId;
        List<String> jsonStrList = redisTemplate.opsForList().range(key, -10, -1);
        List<Message> messages = new ArrayList<>();
        if (jsonStrList != null) {
            for (String jsonStr : jsonStrList) {
                if (jsonStr == null || !jsonStr.trim().startsWith("{")) continue;
                try {
                    JSONObject json = JSONUtil.parseObj(jsonStr);
                    String role = json.getStr("role");
                    String content = json.getStr("content");
                    messages.add("ASSISTANT".equals(role) ? new AssistantMessage(content) : new UserMessage(content));
                } catch (Exception ignored) {}
            }
        }
        return messages;
    }

    @Override
    public void saveSlot(String sessionId, String shopId, String shopName) {
        String key = "chat:slot:" + sessionId;
        redisTemplate.opsForHash().put(key, "current_shop_id", shopId);
        redisTemplate.opsForHash().put(key, "current_shop_name", shopName);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }

    @Override
    public String getCurrentShopName(String sessionId) {
        String key = "chat:slot:" + sessionId;
        return (String) redisTemplate.opsForHash().get(key, "current_shop_name");
    }
}