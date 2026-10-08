package com.rag.service;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ChatHistoryService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "chat:history:";
    private static final int MAX_HISTORY = 10;   // 最多保留 10 条

    /**
     * 保存一条消息
     */
    public void addMessage(String sessionId, String role, String content) {
        String key = KEY_PREFIX + sessionId;
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        stringRedisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(msg));
        // 限制长度
        stringRedisTemplate.opsForList().trim(key, -MAX_HISTORY, -1);
        // 30 分钟过期
        stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }

    /**
     * 获取历史
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);
        List<Map<String, String>> result = new ArrayList<>();
        if (list != null) {
            for (String json : list) {
                result.add(JSONUtil.toBean(json, Map.class));
            }
        }
        return result;
    }

    /**
     * 清空
     */
    public void clear(String sessionId) {
        stringRedisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
