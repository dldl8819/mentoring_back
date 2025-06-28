package com.example.mentoring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${upload.dir:uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 파일들을 정적 리소스로 서빙
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(uploadDir).toAbsolutePath().toString() + "/");
        
        // 기본 정적 리소스 설정
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
