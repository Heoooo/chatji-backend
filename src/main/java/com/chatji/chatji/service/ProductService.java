package com.chatji.chatji.service;

import com.chatji.chatji.client.NaverShoppingClient;
import com.chatji.chatji.client.dto.NaverProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final NaverShoppingClient naverClient;
    private final SynonymService synonymService;

    // Category blacklist
    private static final List<String> BLACKLIST_CATEGORIES = List.of("액세서리", "케이스", "필름", "강화유리", "부품", "소모품", "주변기기", "거치대");

    @Cacheable(value = "search_v16", key = "#keyword + ':' + #sort + ':' + #start + ':' + #minPrice + ':' + #maxPrice")
    public List<ProductResponse> searchProducts(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice) {
        log.info("[v16] Query Expansion Search - Keyword: {}, Sort: {}", keyword, sort);

        try {
            // 1. 동의어 기반 키워드 확장 (자켓 -> 블루종, 점퍼 등)
            List<String> expandedKeywords = synonymService.expandKeyword(keyword);
            log.info("Expanded keywords for {}: {}", keyword, expandedKeywords);

            List<ProductResponse> totalPool = new java.util.ArrayList<>();
            int baseKeywordCount = 0;
            boolean isFirst = true;

            // 2. 확장된 각 키워드별로 검색 수행
            for (String kw : expandedKeywords) {
                NaverProductDto resultDto = naverClient.search(kw, "sim", 1, null, null);
                if (resultDto == null || resultDto.items() == null) continue;

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
                
                if (isFirst) {
                    baseKeywordCount = filtered.size();
                    isFirst = false;
                    log.info("[v16-PROVE] Base keyword '{}' results: {}", kw, baseKeywordCount);
                }
                
                totalPool.addAll(filtered);
            }

            // 3. 중복 제거 및 결과 집계
            List<ProductResponse> uniquePool = totalPool.stream()
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            double improvementFactor = baseKeywordCount > 0 ? (double) uniquePool.size() / baseKeywordCount * 100 : 100;
            log.info("[v16-PROVE] TOTAL Expanded Unique Results: {} (Coverage: {}%)", uniquePool.size(), String.format("%.1f", improvementFactor));

            // 4. 정렬 적용
            if (sort != null && sort.contains("price_")) {
                java.util.Comparator<ProductResponse> comparator = java.util.Comparator.comparingInt(ProductResponse::lprice);
                if (sort.contains("dsc")) comparator = comparator.reversed();
                uniquePool.sort(comparator);
            }

            // 5. 페이징 처리
            int fromIndex = Math.min(start - 1, uniquePool.size());
            int toIndex = Math.min(fromIndex + 100, uniquePool.size());
            return uniquePool.subList(fromIndex, toIndex);

        } catch (Exception e) {
            log.error("[v16] Query Expansion Error: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public record ProductResponse(
            String productId, String title, String link, String image, Integer lprice, String mallName) {
    }
}
