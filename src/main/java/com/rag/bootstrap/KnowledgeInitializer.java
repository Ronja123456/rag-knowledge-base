package com.rag.bootstrap;

import com.rag.service.EmbeddingService;
import com.rag.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class KnowledgeInitializer implements CommandLineRunner {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private ResourceLoader resourceLoader;

    // 你的文档放在 resources/docs/ 下
    private final List<String> docFiles = Arrays.asList(
            "classpath:demo/优惠券使用说明.md",
            "classpath:demo/订单退款与售后政策.md",
            "classpath:demo/商家入驻与合作FAQ.md",
            "classpath:demo/会员等级与积分规则.md",
            "classpath:demo/平台社区发帖规范.md"
    );

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 开始初始化 RAG 知识库 ==========");

        int chunkIndex = 0;
        try {
            for (String filePath : docFiles) {
                Resource resource = resourceLoader.getResource(filePath);
                if (!resource.exists()) {
                    log.warn("文档不存在，跳过: {}", filePath);
                    continue;
                }

                // 1. 读取整篇文档
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                // 2. 简单的段落切片（按 \n 切分，过滤空行）
                String[] paragraphs = content.split("\n");

                for (String paragraph : paragraphs) {
                    String cleanText = paragraph.trim();
                    if (cleanText.isEmpty() || cleanText.startsWith("【")) continue; // 忽略标题行

                    // 3. 向量化
                    float[] vector = embeddingService.embed(cleanText);

                    // 4. 存入向量库 (ID 采用 UUID 防止冲突，KbId 设为 "kb_official")
                    String id = "doc_" + (chunkIndex++);
                  //  vectorStoreService.add(id, "kb_official", cleanText, vector);
                    vectorStoreService.add("kb_official", id, cleanText, vector);

                    log.info("[知识入库] 成功存入段落: {}", cleanText);
                }
            }
            log.info("========== RAG 知识库初始化完成，共入库 {} 个文本块 ==========", chunkIndex);
        } catch (Exception e) {
            log.error("RAG 知识库初始化异常", e);
        }
    }
}