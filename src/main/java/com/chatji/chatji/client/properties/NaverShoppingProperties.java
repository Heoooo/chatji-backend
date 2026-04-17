package com.chatji.chatji.client.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "naver.openapi")
@Data
@Slf4j
public class NaverShoppingProperties {
    private String clientId;
    private String clientSecret;
    private String url;

    @PostConstruct
    public void debugConfig() {
        if (clientId == null || clientId.isEmpty() || clientId.contains("YOUR_CLIENT_ID")) {
            log.error("[AUTH-ERROR] Naver Client ID is EMPTY or INVALID! Please check application-secret.yml");
        } else {
            log.info("[AUTH-CHECK] Naver Client ID loaded: {}***", clientId.substring(0, 3));
            log.info("[AUTH-CHECK] Naver Client Secret length: {}", (clientSecret != null ? clientSecret.length() : 0));
        }
    }
}
