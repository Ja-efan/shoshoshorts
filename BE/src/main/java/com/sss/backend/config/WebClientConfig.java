package com.sss.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
//                .baseUrl("http://localhost:8000")  // FastAPI 기본 URL
                .baseUrl("http://ai-api-1:8000")  // 컨테이너 네트워크에서 접속
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}