package com.rag.controller;

import com.rag.service.ChatHistoryService;
import com.rag.service.DocumentService;
import com.rag.service.EmbeddingService;
import com.rag.service.VectorStoreService;
import com.rag.utils.DeepSeekClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rag")
public class RagController {

    @Resource
    private DocumentService documentService;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private DeepSeekClient deepSeekClient;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestBody Map<String, String> req) {
        String text = req.get("text");
        int chunks = documentService.upload(text);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("chunks", chunks);
        result.put("total", vectorStoreService.size());
        return result;
    }

    /**
     * RAG 问答
     */
    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question,@RequestParam(required = false) String sessionId) {

        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        // ① 提问用 query 类型
        float[] queryVector = embeddingService.embed(question, "query");

        // ② 检索 top 3
        List<VectorStoreService.VectorItem> topK = vectorStoreService.search(queryVector, 3);

        // ③ 拼 prompt
        String context = topK.stream()
                .map(VectorStoreService.VectorItem::getText)
                .collect(Collectors.joining("\n---\n"));

        String prompt = "请根据以下参考资料回答问题。如果资料中没有相关信息，请说'资料中未提及'。\n\n" +
                "参考资料：\n" + context + "\n\n" +
                "问题：" + question;

        // ④ 调 LLM
        List<Map<String, String>> history = chatHistoryService.getHistory(sessionId);
        String answer = deepSeekClient.chat(prompt);

        // 4. 保存对话
        chatHistoryService.addMessage(sessionId, "user", question);
        chatHistoryService.addMessage(sessionId, "assistant", answer);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        // 返回引用 + 相似度分数
        List<Map<String, Object>> refs = topK.stream().map(item -> {
            Map<String, Object> ref = new HashMap<>();
            ref.put("text", item.getText());
            ref.put("score", item.getScore());   // ← 相似度分数
            return ref;
        }).collect(Collectors.toList());
        result.put("references", refs);


        return result;
    }

    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestParam String question,
                                @RequestParam(required = false) String sessionId) {
        // 60 秒超时
        SseEmitter emitter = new SseEmitter(60_000L);

        final String sid = (sessionId == null) ? UUID.randomUUID().toString() : sessionId;

        // 用新线程异步处理，避免阻塞
        new Thread(() -> {
            try {
                // 1. 检索
                float[] queryVector = embeddingService.embed(question, "query");
                List<VectorStoreService.VectorItem> topK = vectorStoreService.search(queryVector, 3);
                String context = topK.stream()
                        .map(VectorStoreService.VectorItem::getText)
                        .collect(Collectors.joining("\n---\n"));

                // 2. 拼 prompt
                String prompt = "请根据以下参考资料回答问题...\n\n参考资料：\n" + context + "\n\n问题：" + question;

                // 3. 取历史
                List<Map<String, String>> history = chatHistoryService.getHistory(sid);

                // 4. 流式调 LLM，逐段推给前端
                StringBuilder fullAnswer = new StringBuilder();
                deepSeekClient.chatStream(prompt, history, chunk -> {
                    try {
                        fullAnswer.append(chunk);
                        emitter.send(chunk);   // ← 推一段
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });

                // 5. 保存对话历史
                chatHistoryService.addMessage(sid, "user", question);
                chatHistoryService.addMessage(sid, "assistant", fullAnswer.toString());

                // 6. 结束
                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
    /**
     * 清空（测试用）
     */
    @PostMapping("/clear")
    public Map<String, Object> clear() {
        vectorStoreService.clear();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}