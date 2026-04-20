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
        if (clientId == null || clientId.isBlank() || clientId.contains("YOUR_CLIENT_ID")) {
            log.error("[AUTH-ERROR] Naver Client ID is EMPTY or INVALID! Current: [{}] - Please check Environment Variables.", clientId);
        } else {
            // v30.5: 안전한 로깅 (글자 수 체크)
            String maskedId = (clientId.length() > 3) ? clientId.substring(0, 3) + "***" : "ID_TOO_SHORT";
            log.info("[AUTH-CHECK] Naver Client ID loaded: {} (Total length: {})", maskedId, clientId.length());
        }
    }
}
