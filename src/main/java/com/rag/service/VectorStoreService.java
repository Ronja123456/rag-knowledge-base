package com.rag.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class VectorStoreService {

    @Resource
    private EmbeddingService embeddingService;

    // 内存向量库（真实场景用 Redis / Milvus）
    private final List<VectorItem> store = new ArrayList<>();

    /**
     * 存入向量
     */
    public void add(String id, String text, float[] vector) {
        VectorItem item = new VectorItem();
        item.setId(id);
        item.setText(text);
        item.setVector(vector);
        store.add(item);
    }

    /**
     * 检索 topK 最相似的
     */
    public List<VectorItem> search(float[] queryVector, int topK) {
        // 小顶堆，保留相似度最高的 topK
        PriorityQueue<VectorItem> heap = new PriorityQueue<>(
                (a, b) -> Float.compare(a.getScore(), b.getScore())
        );

        for (VectorItem item : store) {
            float score = embeddingService.cosineSimilarity(queryVector, item.getVector());
            item.setScore(score);
            if (heap.size() < topK) {
                heap.offer(item);
            } else if (score > heap.peek().getScore()) {
                heap.poll();
                heap.offer(item);
            }
        }

        // 堆里是无序的，转成 list 按 score 降序
        List<VectorItem> result = new ArrayList<>(heap);
        result.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return result;
    }

    /**
     * 清空（测试用）
     */
    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    @Data
    public static class VectorItem {
        private String id;
        private String text;
        private float[] vector;
        private float score;
    }
}