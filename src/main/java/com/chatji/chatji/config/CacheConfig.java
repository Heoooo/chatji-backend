package com.chatji.chatji.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Caffeine properties are configured in application.yml.
    // If further customization is needed, a CaffeineCacheManager bean can be
    // defined.
}
