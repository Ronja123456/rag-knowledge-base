package com.rag.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

//@Component
public class AdminApiInterceptor implements HandlerInterceptor {

    private final DemoProperties demoProperties;

    public AdminApiInterceptor(DemoProperties demoProperties) {
        this.demoProperties = demoProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (demoProperties.isAdminApiEnabled()) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":404,\"message\":\"Not Found\"}");
        return false;
    }
}
