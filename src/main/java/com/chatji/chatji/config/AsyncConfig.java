package com.chatji.chatji.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "apiExecutor")
    public Executor apiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 기본 스레드 수
        executor.setMaxPoolSize(20);  // 최대 스레드 수
        executor.setQueueCapacity(500); // 대기열
        executor.setThreadNamePrefix("ApiTask-");
        executor.initialize();
        return executor;
    }
}
