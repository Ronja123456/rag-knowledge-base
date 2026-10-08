package com.rag.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.embedding-url}")
    private String embeddingUrl;

    @Value("${qwen.embedding-model}")
    private String embeddingModel;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 向量化：textType = "document" 或 "query"
     */
    public float[] embed(String text, String textType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> input = new HashMap<>();
            input.put("texts", new String[]{text});

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("text_type", textType);

            Map<String, Object> body = new HashMap<>();
            body.put("model", embeddingModel);
            body.put("input", input);
            body.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(embeddingUrl, request, String.class);
            JSONObject json = JSONUtil.parseObj(response.getBody());
            JSONObject output = json.getJSONObject("output");
            JSONArray embeddings = output.getJSONArray("embeddings");
            JSONObject first = embeddings.getJSONObject(0);
            JSONArray embeddingArray = first.getJSONArray("embedding");

            float[] vector = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                vector[i] = embeddingArray.getFloat(i);
            }
            normalize(vector);
            return vector;

        } catch (Exception e) {
            log.error("Embedding 调用失败", e);
            return randomVector(1024);
        }
    }

    /**
     * 兼容旧调用：默认 document
     */
    public float[] embed(String text) {
        return embed(text, "document");
    }

    private void normalize(float[] vector) {
        float sum = 0;
        for (float v : vector) sum += v * v;
        float norm = (float) Math.sqrt(sum);
        if (norm == 0) return;
        for (int i = 0; i < vector.length; i++) vector[i] /= norm;
    }

    private float[] randomVector(int dim) {
        float[] vector = new float[dim];
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < dim; i++) vector[i] = random.nextFloat() * 2 - 1;
        normalize(vector);
        return vector;
    }

    public float cosineSimilarity(float[] a, float[] b) {
        float dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}