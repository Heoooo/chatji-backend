package com.chatji.chatji.service;

import com.chatji.chatji.client.NaverShoppingClient;
import com.chatji.chatji.client.dto.NaverProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final NaverShoppingClient naverClient;

    // 🛡️ 10년차 개발자의 노하우: 가짜 상품들이 밀집된 카테고리 블랙리스트
    private static final List<String> BLACKLIST_CATEGORIES = List.of("액세서리", "케이스", "필름", "강화유리", "부품", "소모품", "주변기기", "거치대");

    @Cacheable(value = "search_v10", key = "#keyword + ':' + #sort + ':' + #start + ':' + #minPrice + ':' + #maxPrice")
    public List<ProductResponse> searchProducts(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice) {
        log.info("🔥 스마트 엔진(Smart Re-Sort) 가동: Keyword={}, Sort={}", keyword, sort);

        try {
            // 🚨 '가격 낮은순'일 경우, 정확도순으로 1,000개를 먼저 가져와서 재정렬하는 전략 사용
            if ("price_asc".equals(sort)) {
                List<ProductResponse> allBestSimItems = new java.util.ArrayList<>();
                
                // 1. 정확도순(sim)으로 최상위 1,000개를 수집 (가장 양질의 데이터)
                for (int pageStart = 1; pageStart <= 901; pageStart += 100) {
                    NaverProductDto resultDto = naverClient.search(keyword, "sim", pageStart, null, null);
                    if (resultDto == null || resultDto.items() == null || resultDto.items().isEmpty()) break;

                    List<ProductResponse> filtered = resultDto.items().stream()
                            .filter(item -> {
                                String cat1 = item.category1() != null ? item.category1() : "";
                                String cat2 = item.category2() != null ? item.category2() : "";
                                String cat3 = item.category3() != null ? item.category3() : "";
                                String cat4 = item.category4() != null ? item.category4() : "";
                                String combined = (cat1 + cat2 + cat3 + cat4).toLowerCase();
                                return BLACKLIST_CATEGORIES.stream().noneMatch(combined::contains);
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
                    allBestSimItems.addAll(filtered);
                }

                // 2. 수집된 양질의 데이터 1,000개를 '가격 낮은순'으로 직접 재정렬
                List<ProductResponse> sortedItems = allBestSimItems.stream()
                        .sorted(java.util.Comparator.comparingInt(ProductResponse::lprice))
                        .toList();

                // 3. 페이징 처리 (무한 스크롤 호환)
                int fromIndex = Math.min(start - 1, sortedItems.size());
                int toIndex = Math.min(fromIndex + 100, sortedItems.size());
                
                return sortedItems.subList(fromIndex, toIndex);
            }

            // 그 외(정확도순, 가격 높은순)는 네이버 API의 기본 정렬을 그대로 사용 (빠른 응답)
            String apiSort = "sim";
            if ("price_dsc".equals(sort)) apiSort = "dsc";

            NaverProductDto resultDto = naverClient.search(keyword, apiSort, start, null, null);
            if (resultDto == null || resultDto.items() == null) return java.util.Collections.emptyList();

            return resultDto.items().stream()
                    .filter(item -> {
                        String cat1 = item.category1() != null ? item.category1() : "";
                        String cat2 = item.category2() != null ? item.category2() : "";
                        String cat3 = item.category3() != null ? item.category3() : "";
                        String cat4 = item.category4() != null ? item.category4() : "";
                        String combined = (cat1 + cat2 + cat3 + cat4).toLowerCase();
                        return BLACKLIST_CATEGORIES.stream().noneMatch(combined::contains);
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

        } catch (Exception e) {
            log.error("❌ 제품 검색 중 오류 발생: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public record ProductResponse(
            String productId, String title, String link, String image, Integer lprice, String mallName) {
    }
}
