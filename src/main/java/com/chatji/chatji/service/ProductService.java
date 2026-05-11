package com.chatji.chatji.service;

import com.chatji.chatji.client.NaverShoppingClient;
import com.chatji.chatji.client.dto.NaverProductDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final NaverShoppingClient naverClient;
    private final Executor apiExecutor;

    private static final List<String> BLACKLIST_CATEGORIES = List.of("액세서리", "케이스", "필름", "강화유리", "부품", "소모품", "주변기기", "거치대");

    /**
     * v20: 장애 격리(Circuit Breaker) 도입
     * - 네이버 API 장애 시 시스템 전파를 차단하고 Fallback 로직 실행
     */

    @Cacheable(value = "search_v17", key = "#keyword + ':' + #sort + ':' + #start + ':' + #minPrice + ':' + #maxPrice", sync = true)
    @CircuitBreaker(name = "productSearch", fallbackMethod = "fallbackSearch")
    public List<ProductResponse> searchProducts(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice) {
        
        long startTime = System.currentTimeMillis();
        log.info("[v20-CB] Search Started - Keyword: {}", keyword);

        try {
            List<Integer> pages = List.of(1, 101, 201);
            
            List<CompletableFuture<List<ProductResponse>>> futures = pages.stream()
                .map(pageStart -> CompletableFuture.supplyAsync(() -> {
                    NaverProductDto resultDto = naverClient.search(keyword, "sim", pageStart, null, null);
                    
                    if (resultDto == null || resultDto.items() == null) return new ArrayList<ProductResponse>();
                    
                    return resultDto.items().stream()
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
                                    item.mallName(),
                                    item.category1(),
                                    item.category2()))
                            .filter(p -> (minPrice == null || p.lprice() >= minPrice))
                            .filter(p -> (maxPrice == null || p.lprice() <= maxPrice))
                            .toList();
                }, apiExecutor))
                .toList();

            List<ProductResponse> totalPool = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            if (sort != null && sort.contains("price_")) {
                java.util.Comparator<ProductResponse> comparator = java.util.Comparator.comparingInt(ProductResponse::lprice);
                if (sort.contains("dsc")) comparator = comparator.reversed();
                totalPool.sort(comparator);
            }

            log.info("[v20-TIME] Parallel Search Processed in {}ms", (System.currentTimeMillis() - startTime));

            int fromIndex = Math.min(start - 1, totalPool.size());
            int toIndex = Math.min(fromIndex + 100, totalPool.size());
            return totalPool.subList(fromIndex, toIndex);

        } catch (Exception e) {
            // 이 catch는 Circuit Breaker가 동작하기 전의 일반 예외 처리입니다.
            throw e; 
        }
    }

    /**
     * Fallback Method: 네이버 API 장애 시 호출됨
     * 포트폴리오 포인트: "실물 데이터 대신 빈 리스트를 반환하여 서비스 가용성을 유지(Graceful Degradation)"
     */
    public List<ProductResponse> fallbackSearch(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice, Throwable t) {
        log.error("[v20-FALLBACK] Circuit Breaker Triggered! Message: {}", t.getMessage());
        // 장애 시 빈 리스트를 반환하거나, 고정된 '추천 상품'을 반환할 수 있음
        return Collections.emptyList();
    }

    public record ProductResponse(
            String productId, String title, String link, String image, Integer lprice, String mallName, String categoryLarge, String categorySmall) {
    }
}
