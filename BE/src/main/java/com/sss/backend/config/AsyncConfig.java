package com.sss.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mediaTaskExecutor")
    public Executor mediaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);      // 기본 스레드 풀 크기
        executor.setMaxPoolSize(8);       // 최대 스레드 풀 크기
        executor.setQueueCapacity(100);   // 큐 용량
        executor.setThreadNamePrefix("MediaTask-");  // 스레드 이름 접두사
        executor.initialize();
        return executor;
    }

    @Bean(name = "audioTaskExecutor")
    public Executor audioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 기본 스레드 풀 크기
        executor.setMaxPoolSize(4);       // 최대 스레드 풀 크기
        executor.setQueueCapacity(50);    // 큐 용량
        executor.setThreadNamePrefix("AudioTask-");  // 스레드 이름 접두사
        executor.initialize();
        return executor;
    }

    @Bean(name = "imageTaskExecutor")
    public Executor imageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // 기본 스레드 풀 크기
        executor.setMaxPoolSize(20);       // 최대 스레드 풀 크기
        executor.setQueueCapacity(100);    // 큐 용량
        executor.setThreadNamePrefix("ImageTask-");  // 스레드 이름 접두사
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
