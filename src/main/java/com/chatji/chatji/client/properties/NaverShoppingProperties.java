package com.chatji.chatji.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "naver.openapi")
@Data
public class NaverShoppingProperties {
    private String clientId;
    private String clientSecret;
    private String url;
}
