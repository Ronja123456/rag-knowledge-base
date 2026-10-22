package com.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private int dailyLimit = 20;

    private float similarityThreshold = 0.32f;

    private boolean adminApiEnabled = false;
}
