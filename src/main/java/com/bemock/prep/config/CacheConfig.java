package com.bemock.prep.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS = "products";

    /**
     * In-process Caffeine cache. Bounded so it can't grow without limit; the TTL is only a safety net
     * (e.g. a row edited directly in the DB). Correctness comes from evicting on update/delete.
     * The transaction-aware proxy delays evictions until the DB transaction commits, so a rolled-back
     * update never clears the cache and a reader can't re-cache the old row between eviction and commit.
     */
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager caffeine = new CaffeineCacheManager(PRODUCTS);
        caffeine.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10)));
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}
