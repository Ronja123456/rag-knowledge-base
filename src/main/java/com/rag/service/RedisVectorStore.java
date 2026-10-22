package com.rag.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisVectorStore {

    private static final String VECTOR_KEY = "demo:kb:vectors:";
    private static final String READY_KEY = "demo:kb:ready:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean isReady(String kbId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(READY_KEY + kbId))
                && Boolean.TRUE.equals(stringRedisTemplate.hasKey(VECTOR_KEY + kbId));
    }

    public void save(String kbId, List<VectorStoreService.VectorItem> items) {
        JSONArray array = new JSONArray();
        for (VectorStoreService.VectorItem item : items) {
            JSONObject obj = new JSONObject();
            obj.set("id", item.getId());
            obj.set("kbId", item.getKbId());
            obj.set("text", item.getText());
            obj.set("vector", item.getVector());
            array.add(obj);
        }
        stringRedisTemplate.opsForValue().set(VECTOR_KEY + kbId, array.toString());
        stringRedisTemplate.opsForValue().set(READY_KEY + kbId, "1");
    }

    public List<VectorStoreService.VectorItem> load(String kbId) {
        String json = stringRedisTemplate.opsForValue().get(VECTOR_KEY + kbId);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        JSONArray array = JSONUtil.parseArray(json);
        List<VectorStoreService.VectorItem> items = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            VectorStoreService.VectorItem item = new VectorStoreService.VectorItem();
            item.setId(obj.getStr("id"));
            item.setKbId(obj.getStr("kbId"));
            item.setText(obj.getStr("text"));
            JSONArray vectorArr = obj.getJSONArray("vector");
            float[] vector = new float[vectorArr.size()];
            for (int j = 0; j < vectorArr.size(); j++) {
                vector[j] = vectorArr.getFloat(j);
            }
            item.setVector(vector);
            items.add(item);
        }
        return items;
    }
}
