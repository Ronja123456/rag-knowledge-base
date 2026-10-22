package com.rag.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum PresetKnowledgeBase {
    ECOMMERCE("ecommerce", "电商退换货 FAQ", "退货时效、换货流程、运费与退款到账说明", "demo/ecommerce.txt"),
    EDUCATION("education", "教培课程咨询", "课程介绍、试听报名、师资与退费规则", "demo/education.txt"),
    TAX("tax", "财税公司业务介绍", "代理记账、报税申报、公司注册与服务范围", "demo/tax.txt");

    private final String id;
    private final String name;
    private final String description;
    private final String classpathFile;

    PresetKnowledgeBase(String id, String name, String description, String classpathFile) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.classpathFile = classpathFile;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getClasspathFile() {
        return classpathFile;
    }

    public static Optional<PresetKnowledgeBase> fromId(String id) {
        return Arrays.stream(values()).filter(item -> item.id.equals(id)).findFirst();
    }

    public static List<PresetKnowledgeBase> all() {
        return List.of(values());
    }
}
