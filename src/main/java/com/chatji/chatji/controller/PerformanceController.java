package com.chatji.chatji.controller;

import com.chatji.chatji.client.NaverShoppingClient;
import com.chatji.chatji.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final ProductService productService;
    private final NaverShoppingClient naverClient;

    /**
     * 포트폴리오 딥다이브용 성능 측정 API
     * 동기(순차) vs 비동기(병렬) 처리 시간을 비교합니다.
     */
    @GetMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestParam String keyword) {
        log.info("[BENCHMARK] Starting Performance Test for: {}", keyword);

        // 1. 동기(순차) 처리 시간 측정
        long syncStart = System.currentTimeMillis();
        naverClient.search(keyword, "sim", 1, null, null);
        naverClient.search(keyword, "sim", 101, null, null);
        naverClient.search(keyword, "sim", 201, null, null);
        long syncDuration = System.currentTimeMillis() - syncStart;

        // 2. 비동기(병렬) 처리 시간 측정 (기존 서비스 로직 활용)
        // ※ 캐시 영향을 제거하기 위해 캐시되지 않는 시나리오로 테스트 권장
        long asyncStart = System.currentTimeMillis();
        productService.searchProducts(keyword, "sim", 1, null, null);
        long asyncDuration = System.currentTimeMillis() - asyncStart;

        // 3. 결과 계산
        double improvement = (double) (syncDuration - asyncDuration) / syncDuration * 100;

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("syncDurationMs", syncDuration);
        result.put("asyncDurationMs", asyncDuration);
        result.put("improvementPercentage", String.format("%.2f%%", improvement));
        result.put("note", "비동기 처리를 통해 약 " + Math.round(improvement) + "%의 속도가 향상되었습니다.");

        log.info("[BENCHMARK] Result: Sync={}ms, Async={}ms, Improvement={}%", 
                syncDuration, asyncDuration, String.format("%.2f", improvement));

        return result;
    }
}
