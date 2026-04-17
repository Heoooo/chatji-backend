package com.chatji.chatji.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        // 실전 배포를 위해 테스트 데이터 삽입 로직 제거 완료
    }
}
