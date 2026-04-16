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

    @Cacheable(value = "search", key = "#keyword + ':' + #sort + ':' + #start + ':' + #minPrice + ':' + #maxPrice")
    public List<ProductResponse> searchProducts(String keyword, String sort, int start, Integer minPrice,
            Integer maxPrice) {
        log.info("외부 API 호출: Keyword={}, Min={}, Max={}", keyword, minPrice, maxPrice);

        String apiSort = "sim";
        if ("price_asc".equals(sort))
            apiSort = "asc";
        else if ("price_dsc".equals(sort))
            apiSort = "dsc";

        NaverProductDto resultDto = naverClient.search(keyword, apiSort, start, minPrice, maxPrice);

        if (resultDto == null || resultDto.items() == null) {
            return Collections.emptyList();
        }

        return resultDto.items().stream()
                .map(item -> new ProductResponse(
                        item.productId(),
                        item.title().replaceAll("<.*?>", ""),
                        item.link(),
                        item.image(),
                        item.lprice(),
                        item.mallName()))
                .collect(Collectors.toList());
    }

    public record ProductResponse(
            String productId, String title, String link, String image, Integer lprice, String mallName) {
    }
}
