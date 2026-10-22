package com.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /*============ 暂时注释掉拦截器 ============
    private final AdminApiInterceptor adminApiInterceptor;

    public WebMvcConfig(AdminApiInterceptor adminApiInterceptor) {
        this.adminApiInterceptor = adminApiInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiInterceptor)
                .addPathPatterns("/rag/**", "/chat/**");
    }
     ==========================================
     */

    @Bean(name = "demoTaskExecutor")
    public Executor demoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("demo-sse-");
        executor.initialize();
        return executor;
    }
}
