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

    private static final List<String> BLACKLIST_CATEGORIES = List.of("액세서리", "케이스", "필름", "강화유리", "부품", "소모품", "주변기기", "거치대");

    @Cacheable(value = "search_v15", key = "#keyword + ':' + #sort + ':' + #start + ':' + #minPrice + ':' + #maxPrice")
    public List<ProductResponse> searchProducts(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice) {
        log.info("[v15] Search - Keyword: {}, Sort: {}, Start: {}, Price: {}~{}", 
                keyword, sort, start, minPrice, maxPrice);

        try {
            List<ProductResponse> totalPool = new java.util.ArrayList<>();
            
            for (int pageStart = 1; pageStart <= 201; pageStart += 100) {
                NaverProductDto resultDto = naverClient.search(keyword, "sim", pageStart, null, null);
                if (resultDto == null || resultDto.items() == null || resultDto.items().isEmpty()) break;

                log.info("Page {} count: {}", pageStart, resultDto.items().size());

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
                
                totalPool.addAll(filtered);
            }

            log.info("Total pool: {}", totalPool.size());

            if (sort != null && sort.contains("price_")) {
                java.util.Comparator<ProductResponse> comparator = java.util.Comparator.comparingInt(ProductResponse::lprice);
                if (sort.contains("dsc")) comparator = comparator.reversed();
                totalPool.sort(comparator);
            }

            int fromIndex = Math.min(start - 1, totalPool.size());
            int toIndex = Math.min(fromIndex + 100, totalPool.size());
            return totalPool.subList(fromIndex, toIndex);

        } catch (Exception e) {
            log.error("[v14] Error: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public record ProductResponse(
            String productId, String title, String link, String image, Integer lprice, String mallName) {
    }
}
