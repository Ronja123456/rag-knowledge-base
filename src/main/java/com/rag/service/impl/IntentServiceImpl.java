package com.rag.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rag.dto.IntentResult;
import com.rag.service.IntentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class IntentServiceImpl implements IntentService {

    private final ChatClient chatClient;
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private static final String CLASSIFY_PROMPT = """
            你是一个意图分类器。请严格判断用户输入，输出纯净的JSON，不要包含任何markdown标记。
            JSON格式: {"intent": "查商铺/闲聊/越界", "confidence": 0.0~1.0}
            查询特定店铺、地址、名称，为"查商铺"。
            查询优惠券、退单、会员、平台规则，为"规则咨询"。
            涉及医疗、政治、法律等必须为"越界"。
            用户输入：%s
            """;

    public IntentServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public IntentResult classify(String message) {
        IntentResult result = new IntentResult();
        result.setIntent("闲聊");
        result.setConfidence(1.0);

        try {
            String rawResponse = chatClient.prompt().user(String.format(CLASSIFY_PROMPT, message)).call().content();
            Matcher matcher = JSON_PATTERN.matcher(rawResponse);
            if (matcher.find()) {
                JSONObject json = JSONUtil.parseObj(matcher.group());
                result.setIntent(json.getStr("intent"));
                result.setConfidence(json.getDouble("confidence"));
            }
        } catch (Exception e) {
            log.error("意图解析异常，降级为闲聊放行", e);
        }
        return result;
    }
}