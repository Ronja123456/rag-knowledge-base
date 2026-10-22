package com.rag.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rag.entity.Shop;
import com.rag.service.IShopService;
import com.rag.service.MemoryService;
import com.rag.service.VectorStoreService; // 👈 引入你原来的 RAG 服务
import com.rag.utils.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AgentTools {

    @Autowired
    private IShopService shopService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private VectorStoreService vectorStoreService; //  注入 RAG 检索服务


    //  新增：注入向量化服务
    @Autowired
    private com.rag.service.EmbeddingService embeddingService;


    /**
     * 工具一：查商铺（MySQL）
     */
    @Tool(description = "根据商铺ID查询商铺详情，包括名称、地址。当用户问特定商铺信息时，必须调用此工具")
    public String getShopInfo(Long shopId) {
        long startTime = System.currentTimeMillis();
        String status = "UNKNOWN";

        try {
            // 1. 参数校验
            if (shopId == null || shopId <= 0) {
                status = "INVALID_PARAM";
                return buildErrorJson("商铺ID不合法，必须为正整数");
            }

            // 2. 查库
            Shop shop = shopService.getById(shopId);
            if (shop == null) {
                status = "EMPTY";
                return buildErrorJson("数据库中未找到ID为 " + shopId + " 的商铺");
            }

            // 3. 存入长期槽位（Slot Memory），用于多轮指代
            String sessionId = SessionContext.getSessionId();
            if (sessionId != null) {
                memoryService.saveSlot(sessionId, shopId.toString(), shop.getName());
            }

            // 4. 成功返回
            status = "SUCCESS";
            JSONObject successJson = JSONUtil.createObj();
            successJson.set("status", status);
            successJson.set("data", String.format("商铺名称：%s，地址：%s", shop.getName(), shop.getAddress()));
            return successJson.toString();

        } catch (Exception e) {
            status = "ERROR";
            log.error("[Tool] getShopInfo 异常, shopId: {}", shopId, e);
            return buildErrorJson("查询商铺系统异常，请稍后再试");
        } finally {
            // 核心：统一埋点日志，方便后续 grep 统计失败率
            long cost = System.currentTimeMillis() - startTime;
            log.info("[Tool_Metrics] name=getShopInfo, param={}, status={}, cost={}ms", shopId, status, cost);
        }
    }


    /**
     * 工具二：查知识库/规则（RAG）
     */
    @Tool(description = "查询平台规则、优惠券使用说明等知识库文档。当用户问如何使用优惠券、退单规则时使用")
    public String searchDocs(String query) {
        long startTime = System.currentTimeMillis();
        String status = "UNKNOWN";

        try {
            // 1. 参数校验
            if (query == null || query.trim().isEmpty()) {
                status = "INVALID_PARAM";
                return buildErrorJson("查询内容不能为空");
            }

            // 👇 核心修改：调用 embeddingService 把文本转成向量
            // ⚠️ 注意：你的老项目里，把字符串变成向量的方法叫什么？
            // 是 getEmbedding(query)? 还是 embed(query)? 还是 textToVector(query)?
            // 请把下面的 getEmbedding 换成你老项目里真实存在的方法名！
            float[] queryVector = embeddingService.embed(query);

            // 然后用转出来的向量去查库
            /*
            List<VectorStoreService.VectorItem> items = vectorStoreService.search(queryVector, 3);
*/
            // 把原来的 search(queryVector, 3) 改成这样：
            List<VectorStoreService.VectorItem> items = vectorStoreService.search("kb_official", queryVector, 3);
            if (items == null || items.isEmpty()) {
                status = "EMPTY";
                return buildErrorJson("知识库中未找到与该问题相关的规则");
            }

            // 拼接检索结果
            StringBuilder sb = new StringBuilder();
            for (VectorStoreService.VectorItem item : items) {
                sb.append(item.getText()).append("\n---\n");
            }

            // 成功返回
            status = "SUCCESS";
            JSONObject successJson = JSONUtil.createObj();
            successJson.set("status", status);
            successJson.set("data", sb.toString());
            return successJson.toString();

        } catch (Exception e) {
            status = "ERROR";
            log.error("[Tool] searchDocs 异常, query: {}", query, e);
            return buildErrorJson("知识库检索服务异常");
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            log.info("[Tool_Metrics] name=searchDocs, query={}, status={}, cost={}ms", query, status, cost);
        }
    }



    /**
     * 构造统一错误 JSON 的方法
     */
    private String buildErrorJson(String message) {
        JSONObject errorJson = JSONUtil.createObj();
        // 为了兼容你前面的 System Prompt 约束，这里状态统称为 EMPTY 也可以，
        // 或者根据实际传递，但确保大模型能看懂要拒答/如实说
        errorJson.set("status", "EMPTY");
        errorJson.set("message", message);
        return errorJson.toString();
    }
}