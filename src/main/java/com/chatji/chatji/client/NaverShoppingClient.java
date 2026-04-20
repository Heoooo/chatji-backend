package com.chatji.chatji.client;

import com.chatji.chatji.client.dto.NaverProductDto;
import com.chatji.chatji.client.properties.NaverShoppingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.Optional;

@Slf4j
@Component
public class NaverShoppingClient {
    private final RestClient restClient;

    public NaverShoppingClient(RestClient.Builder restClientBuilder, NaverShoppingProperties properties) {
        // v30.5: 로그를 통해 어떤 키값이 주입되어 있는지 최종 확인
        log.info("[NAVER-CLIENT-INIT] BaseURL: {}", properties.getUrl());
        
        this.restClient = restClientBuilder
                .baseUrl(properties.getUrl())
                .defaultHeader("X-Naver-Client-Id", properties.getClientId())
                .defaultHeader("X-Naver-Client-Secret", properties.getClientSecret())
                .build();
    }

    @Retryable(retryFor = { Exception.class }, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public NaverProductDto search(String keyword, String sort, int start, Integer minPrice, Integer maxPrice) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", keyword)
                            .queryParam("display", 100)
                            .queryParam("start", start)
                            .queryParam("sort", sort)
                            .queryParamIfPresent("lprice", Optional.ofNullable(minPrice))
                            .queryParamIfPresent("hprice", Optional.ofNullable(maxPrice))
                            .build())
                    .retrieve()
                    .body(NaverProductDto.class);
        } catch (RestClientResponseException e) {
            log.error("[NAVER-API-FAIL] HTTP Status: {}, Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[NAVER-API-ERROR] Type: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
