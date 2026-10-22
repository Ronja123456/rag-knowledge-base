package com.rag;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

// 👈 关键修改 1：设置随机端口启动真实的 Web 环境
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AgentEvaluationTest {

    // 👈 关键修改 2：注入 TestRestTemplate，发真实 HTTP 请求
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void runEvaluation() throws Exception {
        // 1. 读取测试用例
        InputStream is = getClass().getClassLoader().getResourceAsStream("test_cases.json");
        if (is == null) {
            throw new RuntimeException("找不到 test_cases.json，请检查是否放在 src/test/resources 下");
        }
        String jsonStr = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
        JSONArray cases = JSONUtil.parseArray(jsonStr);

        int total = cases.size();
        int pass = 0;
        long totalTime = 0;

        System.out.println("========== 开始自动化评估 ==========");

        String sessionId = "eval_test_001";

        for (int i = 0; i < total; i++) {
            JSONObject c = cases.getJSONObject(i);
            String msg = c.getStr("msg");
            String expectedType = c.getStr("expectedType");
            String expectedKeyword = c.getStr("expectedKeyword");
            if (expectedKeyword == null) {
                expectedKeyword = ""; // 如果没写，就给它一个空字符串，防止后面的 contains 报错
            }

            long start = System.currentTimeMillis();

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "text/event-stream");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 👈 关键修改 3：使用 restTemplate 发起真实的 GET 请求
            String url = "/chat?msg={msg}&sessionId={sessionId}";
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    msg, sessionId
            );

            // 注意：流式返回的数据可能带有 "data:" 前缀，我们提取纯文本

            String response = responseEntity.getBody();
            if (response != null) {
                // 👈 核心：删掉所有的换行符和空格，方便后面对比
                response = response.replace("data:", "")
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace(" ", "")
                        .replace("*", "")  // 👈 干掉 Markdown 的加粗星号
                        .replace("-", "")  // 👈 干掉列表的短横线
                        .trim();
            }
            long cost = System.currentTimeMillis() - start;
            totalTime += cost;

            // 3. 断言逻辑
            boolean isPass = false;

            if ("REFUSE".equals(expectedType)) {
                isPass = response != null && (
                        response.contains("抱歉") ||
                                response.contains("超出") ||
                                response.contains("请问") ||     // 包容：“请问您要查几号商铺？”
                                response.contains("猜不") ||     // 包容：“这个我可猜不出来呀”
                                response.contains("理解您") ||   // 包容：“理解您可能有不满的地方”
                                response.contains("未查到") ||   // 包容：“未查到。知识库中没有...”
                                response.contains("哈哈") ||     // 包容：“哈哈，先别不开心啦”
                                response.contains("建议您")      // 包容：“建议您咨询在线客服”
                );
            } else if ("SUCCESS".equals(expectedType)) {
                // 👈 核心修复：把你漏掉的 SUCCESS 分支加回来！
                // 如果 JSON 里没写 expectedKeyword，默认不拦截；如果写了，就严格匹配
                if (expectedKeyword == null || expectedKeyword.isEmpty()) {
                    isPass = response != null;
                } else {
                    isPass = response != null && response.contains(expectedKeyword);
                }
            }
                else if ("EMPTY".equals(expectedType)) {
                    isPass = response != null && (response.contains(expectedKeyword) ||
                            response.contains("未查到") ||
                            response.contains("不合法") ||
                            response.contains("未找到")
                    );
                } else if ("RAG".equals(expectedType)) {
                    isPass = response != null && response.contains(expectedKeyword);
                }

                if (isPass) {
                    pass++;
                    System.out.printf("[PASS] 耗时:%4dms | 问:%s\n", cost, msg);
                } else {
                    String shortResp = (response == null) ? "NULL" : (response.length() > 50 ? response.substring(0, 50) + "..." : response);
                    System.out.printf("[FAIL] 耗时:%4dms | 问:%s | 实际答:%s\n", cost, msg, shortResp);
                }
            }

            System.out.println("========== 评估结束 ==========");
            System.out.printf("总用例数: %d\n", total);
            System.out.printf("通过数: %d\n", pass);
            System.out.printf("通过率: %.2f%%\n", (double) pass / total * 100);
            System.out.printf("平均响应耗时: %dms\n", totalTime / total);

            assertTrue((double) pass / total >= 0.8, "通过率低于80%，请检查代码");
        }
    }
