package com.rag.service;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final int CHUNK_SIZE = 200;
    private static final int OVERLAP = 50;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private VectorStoreService vectorStoreService;

    public int upload(String text) {
        return upload("default", text);
    }

    public int upload(String kbId, String text) {
        List<String> chunks = split(text);
        for (String chunk : chunks) {
            float[] vector = embeddingService.embed(chunk, "document");
            vectorStoreService.add(kbId, UUID.randomUUID().toString(), chunk, vector);
        }
        return chunks.size();
    }

    List<String> split(String text) {
        String normalized = text.replace("\r\n", "\n").trim();
        String[] paragraphs = normalized.split("\n\\s*\n");
        List<String> chunks = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String p = paragraph.replaceAll("\\s+", " ").trim();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= CHUNK_SIZE) {
                chunks.add(p);
                continue;
            }
            int step = Math.max(CHUNK_SIZE - OVERLAP, 1);
            for (int i = 0; i < p.length(); i += step) {
                int end = Math.min(i + CHUNK_SIZE, p.length());
                chunks.add(p.substring(i, end));
                if (end == p.length()) {
                    break;
                }
            }
        }
        return chunks;
    }
}
