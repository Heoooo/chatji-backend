package com.chatji.chatji.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * [Local Profile] 로컬 환경에서는 방화벽 제약이 없는 로컬 메모리 캐시(Caffeine)를 사용합니다.
     */
    @Bean
    @Profile("local")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // v18에서 설계했던 멀티 TTL 정책 계승
        cacheManager.registerCustomCache("search_v17", 
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());

        cacheManager.registerCustomCache("price_check", 
            Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .maximumSize(500)
                .build());

        return cacheManager;
    }

    /**
     * [Prod Profile] 실서버(배포) 환경에서는 다중 서버 확장을 위해 Redis 전역 캐시를 사용합니다.
     */
    @Bean
    @Profile("prod")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        configurations.put("search_v17", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configurations.put("price_check", defaultConfig.entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configurations)
                .build();
    }
}
