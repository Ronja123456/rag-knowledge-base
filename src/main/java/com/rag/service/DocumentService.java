package com.rag.service;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private VectorStoreService vectorStoreService;

    // 切分大小（字符数）
    private static final int CHUNK_SIZE = 200;

    /**
     * 上传文档：切分 → 向量化 → 存
     */
    public int upload(String text) {
        List<String> chunks = split(text);
        for (String chunk : chunks) {
            float[] vector = embeddingService.embed(chunk, "document");
            vectorStoreService.add(UUID.randomUUID().toString(), chunk, vector);
        }
        return chunks.size();
    }

    /**
     * 切分文本
     */
    private List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        // 按标点/换行切，或者按固定长度切
        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }
}