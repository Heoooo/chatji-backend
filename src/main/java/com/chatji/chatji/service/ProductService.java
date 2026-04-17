package com.chatji.chatji.service;

import com.chatji.chatji.client.NaverShoppingClient;
import com.chatji.chatji.client.dto.NaverProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final NaverShoppingClient naverClient;
    private final Executor apiExecutor; // AsyncConfig에서 정의한 빈

    private static final List<String> BLACKLIST_CATEGORIES = List.of("액세서리", "케이스", "필름", "강화유리", "부품", "소모품", "주변기기", "거치대");

    /**
     * v19: 비동기 병렬 처리(CompletableFuture) 엔진 도입
     * - 순차적 API 호출을 병렬 처리로 전환하여 Latency를 획기적으로 단축 (IO Bound 작업 최적화)
     */
    @Cacheable(value = "search_v17", key = "#keyword + ':' + #sort + ':' + #start + ':' + #minPrice + ':' + #maxPrice", sync = true)
    public List<ProductResponse> searchProducts(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice) {
        
        long startTime = System.currentTimeMillis();
        log.info("[v19-ASYNC] Parallel Search Started - Keyword: {}", keyword);

        try {
            // 🚨 [병렬 처리] 1, 101, 201 페이지 요청을 동시에 수행
            List<Integer> pages = List.of(1, 101, 201);
            
            List<CompletableFuture<List<ProductResponse>>> futures = pages.stream()
                .map(pageStart -> CompletableFuture.supplyAsync(() -> {
                    long taskStart = System.currentTimeMillis();
                    NaverProductDto resultDto = naverClient.search(keyword, "sim", pageStart, null, null);
                    
                    if (resultDto == null || resultDto.items() == null) return new ArrayList<ProductResponse>();
                    
                    List<ProductResponse> filtered = resultDto.items().stream()
                            .filter(item -> {
                                String cat = (item.category1() + item.category2() + item.category3() + item.category4()).toLowerCase();
                                return BLACKLIST_CATEGORIES.stream().noneMatch(cat::contains);
                            })
                            .map(item -> new ProductResponse(
                                    item.productId(),
                                    item.title().replaceAll("<.*?>", ""),
                                    item.link(),
                                    item.image(),
                                    item.lprice(),
                                    item.mallName()))
                            .filter(p -> (minPrice == null || p.lprice() >= minPrice))
                            .filter(p -> (maxPrice == null || p.lprice() <= maxPrice))
                            .toList();
                    
                    log.info("[v19-TASK] Page {} fetched in {}ms", pageStart, (System.currentTimeMillis() - taskStart));
                    return filtered;
                }, apiExecutor))
                .toList();

            // 모든 비동기 작업 결과 대기 및 병합
            List<ProductResponse> totalPool = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            // 정렬 적용
            if (sort != null && sort.contains("price_")) {
                java.util.Comparator<ProductResponse> comparator = java.util.Comparator.comparingInt(ProductResponse::lprice);
                if (sort.contains("dsc")) comparator = comparator.reversed();
                totalPool.sort(comparator);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[v19-TIME] Total Parallel Process Time: {}ms", duration);

            int fromIndex = Math.min(start - 1, totalPool.size());
            int toIndex = Math.min(fromIndex + 100, totalPool.size());
            return totalPool.subList(fromIndex, toIndex);

        } catch (Exception e) {
            log.error("[v19] Async Search Error: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public record ProductResponse(
            String productId, String title, String link, String image, Integer lprice, String mallName) {
    }
}
