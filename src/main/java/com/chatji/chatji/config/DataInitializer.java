package com.chatji.chatji.config;

import com.chatji.chatji.domain.hotdeal.HotDeal;
import com.chatji.chatji.domain.hotdeal.HotDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HotDealRepository hotDealRepository;

    @Override
    public void run(String... args) throws Exception {
        // UI 확인을 위한 테스트 데이터 삽입
        if (hotDealRepository.count() == 0) {
            HotDeal dummy = HotDeal.builder()
                    .originId("dummy-001")
                    .title("[테스트] 삼성 오디세이 G5 게이밍 모니터 (역대급 특가!)")
                    .url("https://www.naver.com")
                    .currentPrice(298000)
                    .source("뿜뿌")
                    .mallName("G마켓")
                    .build();
            
            hotDealRepository.save(dummy);
        }
    }
}
