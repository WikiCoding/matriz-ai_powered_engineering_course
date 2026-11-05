package com.wikicoding.matrizbackend.apigateway.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontendConfiguration implements WebMvcConfigurer {
    private static final String[] PAGE_PATHS = {
            "/", "/projects", "/projects/{projectId}", "/tasks/{taskId}", "/users", "/teams"
    };

    static final String[] PUBLIC_PATHS = {
            "/", "/projects", "/projects/*", "/tasks/*", "/users", "/teams",
            "/index.html", "/static/**", "/favicon.ico", "/manifest.json",
            "/asset-manifest.json", "/robots.txt", "/logo192.png", "/logo512.png"
    };

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String path : PAGE_PATHS) {
            registry.addViewController(path).setViewName("forward:/index.html");
        }
    }
}
