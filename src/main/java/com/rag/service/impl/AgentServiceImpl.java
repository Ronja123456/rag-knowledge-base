
package com.rag.service.impl;

import com.rag.dto.IntentResult;
import com.rag.service.AgentService;
import com.rag.service.IntentService;
import com.rag.service.MemoryService;
import com.rag.tools.AgentTools;
import com.rag.utils.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final IntentService intentService;
    private final MemoryService memoryService;
    private final AgentTools agentTools;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public AgentServiceImpl(ChatClient.Builder builder, IntentService intentService,
                            MemoryService memoryService, AgentTools agentTools) {
        this.chatClient = builder.build();
        this.intentService = intentService;
        this.memoryService = memoryService;
        this.agentTools = agentTools;
    }

    @Override
    public SseEmitter streamChat(String message, String sessionId) {
        // 设置超时时间为 2 分钟（大模型生成长文本可能较慢）
        SseEmitter emitter = new SseEmitter(120000L);
        SessionContext.setSessionId(sessionId);



        // 用线程池处理整个流程，避免阻塞 Tomcat 的主线程
        executor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();

/*

                // 1. 并行：意图识别 + 获取历史记忆
                CompletableFuture<IntentResult> intentFuture = CompletableFuture.supplyAsync(() -> intentService.classify(message), executor);
                CompletableFuture<List<Message>> historyFuture = CompletableFuture.supplyAsync(() -> memoryService.getHistory(sessionId), executor);

                IntentResult intentResult = intentFuture.join();
                List<Message> history = historyFuture.join();

 */
                long t1 = System.currentTimeMillis();
                CompletableFuture<IntentResult> intentFuture = CompletableFuture.supplyAsync(() -> intentService.classify(message), executor);
                CompletableFuture<List<Message>> historyFuture = CompletableFuture.supplyAsync(() -> memoryService.getHistory(sessionId), executor);
                CompletableFuture<Void> summaryFuture = CompletableFuture.runAsync(this::mockSummaryHistory, executor); // 👈 并行加入

                IntentResult intentResult = intentFuture.join();
                List<Message> history = historyFuture.join();
                summaryFuture.join();
                long t2 = System.currentTimeMillis();
                log.info("[性能测试] 并行前置耗时: {} ms", (t2 - t1));



                // 2. 硬逻辑拦截
                if ("越界".equals(intentResult.getIntent()) || intentResult.getConfidence() < 0.8) {
                    log.warn("[拦截] 意图: {}, 置信度: {}", intentResult.getIntent(), intentResult.getConfidence());
                    emitter.send(SseEmitter.event().data("抱歉，这个问题超出我的能力范围，可以帮您转接人工客服。"));
                    emitter.complete();
                    return;
                }

                // 3. 获取长期槽位，拼接 System Prompt
                String currentShopName = memoryService.getCurrentShopName(sessionId);
                String systemPrompt = "你是一个本地生活客服。如果工具返回status是EMPTY，必须回复未查到，严禁编造。";
                if (currentShopName != null) {
                    systemPrompt += "\n【系统提示：用户当前关注的商铺是“" + currentShopName + "”。代词默认指代此商铺。】";
                }

                log.info("前置准备耗时: {} ms", System.currentTimeMillis() - startTime);

                // 4. 🚀 发起流式大模型调用
                Flux<String> stream = chatClient.prompt()
                        .system(systemPrompt)
                        .messages(history)
                        .user(message)
                        .tools(agentTools)
                        .stream()
                        .content();

                // 用于拼接完整的 AI 回复（以便流结束后存入 Redis）
                StringBuilder fullResponseBuilder = new StringBuilder();

                // 5. 订阅流并推送给前端
                stream.subscribe(
                        data -> {
                            try {
                                // 逐字推送给前端
                                emitter.send(SseEmitter.event().data(data));
                                fullResponseBuilder.append(data);
                            } catch (IOException e) {
                                log.error("SSE 推送异常，客户端可能已断开", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("大模型流式生成异常", error);
                            emitter.completeWithError(error);
                        },
                        () -> {
                            // 流式输出完成
                            String fullResponse = fullResponseBuilder.toString();
                            // 异步保存完整对话到 Redis
                            CompletableFuture.runAsync(() -> {
                                memoryService.saveHistory(sessionId, "USER", message);
                                memoryService.saveHistory(sessionId, "ASSISTANT", fullResponse);
                            }, executor);

                            log.info("流式输出完成，总耗时: {} ms", System.currentTimeMillis() - startTime);
                            emitter.complete();
                        }
                );

            } catch (Exception e) {
                log.error("Agent 流式处理异常", e);
                try {
                    emitter.send(SseEmitter.event().data("系统繁忙，请稍后再试。"));
                } catch (IOException ex) {
                    log.error("错误信息推送失败", ex);
                }
                emitter.completeWithError(e);
            } finally {
                SessionContext.clear();
            }
        });



        return emitter;
    }
    private void mockSummaryHistory() {
        try {
            // 模拟调用大模型做历史摘要，耗时约 600ms
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}