package com.sss.backend.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {

        // 필요한 경우 더 큰 응답을 처리하기 위해 버퍼 크기 증가
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        // 타임아웃 설정 (10분)
        int timeoutSeconds = 600; // 10분

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // 연결 타임아웃 60초
                .responseTimeout(Duration.ofSeconds(timeoutSeconds)) // 응답 타임아웃 10분
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                );

        return builder
                .exchangeStrategies(exchangeStrategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .baseUrl("http://localhost:8000")  // FastAPI 기본 URL
                .baseUrl("http://35.216.58.38:8000")  // 컨테이너 네트워크에서 접속
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient webClient8001(WebClient.Builder builder) {
        return builder
                .baseUrl("http://35.216.58.38:8001")  // 컨테이너 네트워크에서 접속
                .defaultHeader("Content-Type", "application/json")
                .build();

    }

}