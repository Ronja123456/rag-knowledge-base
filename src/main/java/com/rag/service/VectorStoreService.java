package com.rag.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorStoreService {

    private static final String DEFAULT_KB = "default";

    @Resource
    private EmbeddingService embeddingService;

    private final Map<String, List<VectorItem>> store = new ConcurrentHashMap<>();

    public void add(String id, String text, float[] vector) {
        add(DEFAULT_KB, id, text, vector);
    }

    public void add(String kbId, String id, String text, float[] vector) {
        VectorItem item = new VectorItem();
        item.setId(id);
        item.setKbId(kbId);
        item.setText(text);
        item.setVector(vector);
        store.computeIfAbsent(kbId, key -> new ArrayList<>()).add(item);
    }

    public void replaceAll(String kbId, List<VectorItem> items) {
        store.put(kbId, new ArrayList<>(items));
    }

    public List<VectorItem> snapshot(String kbId) {
        List<VectorItem> items = store.get(kbId);
        if (items == null) {
            return List.of();
        }
        return new ArrayList<>(items);
    }

    public List<VectorItem> search(float[] queryVector, int topK) {
        return search(DEFAULT_KB, queryVector, topK);
    }

    public List<VectorItem> search(String kbId, float[] queryVector, int topK) {
        List<VectorItem> items = store.getOrDefault(kbId, List.of());
        PriorityQueue<VectorItem> heap = new PriorityQueue<>(
                (a, b) -> Float.compare(a.getScore(), b.getScore())
        );
      //  System.out.println("====== [向量调试] 当前查询的 kbId: " + kbId + ", 获取到的向量总数: " + items.size() + " ======");
        for (VectorItem item : items) {
            float score = embeddingService.cosineSimilarity(queryVector, item.getVector());
            VectorItem ranked = copyWithoutVector(item);
            ranked.setScore(score);
            if (heap.size() < topK) {
                heap.offer(ranked);
            } else if (!heap.isEmpty() && score > heap.peek().getScore()) {
                heap.poll();
                heap.offer(ranked);
            }
        }

        List<VectorItem> result = new ArrayList<>(heap);
        result.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return result;
    }

    public void clear() {
        store.clear();
    }

    public void clear(String kbId) {
        store.remove(kbId);
    }

    public int size() {
        return store.values().stream().mapToInt(List::size).sum();
    }

    public int size(String kbId) {
        List<VectorItem> items = store.get(kbId);
        return items == null ? 0 : items.size();
    }

    private VectorItem copyWithoutVector(VectorItem source) {
        VectorItem copy = new VectorItem();
        copy.setId(source.getId());
        copy.setKbId(source.getKbId());
        copy.setText(source.getText());
        return copy;
    }

    @Data
    public static class VectorItem {
        private String id;
        private String kbId;
        private String text;
        private float[] vector;
        private float score;
    }
}
