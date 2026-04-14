package com.chatji.chatji.client;

import com.chatji.chatji.client.dto.NaverProductDto;
import com.chatji.chatji.client.properties.NaverShoppingProperties;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NaverShoppingClient {
    private final RestClient restClient;

    public NaverShoppingClient(RestClient.Builder restClientBuilder, NaverShoppingProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getUrl())
                .defaultHeader("X-Naver-Client-Id", properties.getClientId())
                .defaultHeader("X-Naver-Client-Secret", properties.getClientSecret())
                .build();
    }

    @Retryable(retryFor = { RestClientException.class }, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public NaverProductDto search(String keyword, String sort) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("query", keyword)
                        .queryParam("display", 20)
                        .queryParam("sort", sort)
                        .build())
                .retrieve()
                .body(NaverProductDto.class);
    }
}
