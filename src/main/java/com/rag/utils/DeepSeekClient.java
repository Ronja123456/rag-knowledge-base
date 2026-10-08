package com.rag.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class DeepSeekClient {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    @Resource
    private RestTemplate restTemplate;

    public String chat(String prompt) {
        return  chat(prompt, null);
    }

    /**
     * 多轮对话
     *
     * @param prompt  当前问题
     * @param history 历史消息，格式：
     *                [{"role":"user","content":"..."}, {"role":"assistant","content":"..."}]
     */
    public String chat(String prompt, List<Map<String, String>> history) {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. 组装 messages
        List<Map<String, String>> messages = new ArrayList<>();

        // 2.1 加历史消息
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        // 2.2 加当前问题
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        // 3. 请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);


        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JSONObject json = JSONUtil.parseObj(response.getBody());
            JSONArray choices = json.getJSONArray("choices");
            JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
            return msg.getStr("content");
        } catch (Exception e) {
            log.error("调用 DeepSeek 失败", e);
            return "抱歉，AI 服务暂时不可用";
        }
    }
    public void chatStream(String prompt,
                           List<Map<String, String>> history,
                           java.util.function.Consumer<String> onMessage ) {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", true);   // ← 开启流式
        // ... 组装messages
        List<Map<String, String>> messages = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.put("messages", messages);
        // 用 RestTemplate 的 execute 处理流
        restTemplate.execute(url, HttpMethod.POST,
                request -> {
                    request.getHeaders().putAll(headers);
                    request.getBody().write(JSONUtil.toJsonStr(body).getBytes());
                },
                response -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) break;
                                // 解析 delta
                                JSONObject json = JSONUtil.parseObj(data);
                                String content = json.getJSONArray("choices")
                                        .getJSONObject(0).getJSONObject("delta").getStr("content");
                                if (content != null) {
                                    onMessage.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("流式读取失败", e);
                    }
                    return null;
                });
    }
}