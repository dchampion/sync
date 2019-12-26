package com.dchampion.spring.http;

import java.time.Duration;
import java.util.UUID;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * An in-process implementation of {@link ResponseCache}. This implementation
 * will be injected into instances of {@link AsyncRequestHandler} if the property
 * {@code async.response_cache.scope} is set to {@code in-process}.
 * 
 * @param <T> the type of the body of a {@link ResponseEntity}.
 * 
 * @see SharedResponseCache
 */
@Component
@ConditionalOnProperty(name = "async.response_cache.scope", havingValue = "in-process", matchIfMissing = true)
class InProcessResponseCache<T> implements ResponseCache<T> {

    private Cache<UUID, ResponseEntity<T>> cache = 
            CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(180))
            .build();

    @Override
    public void put(UUID key, ResponseEntity<T> response) {
        cache.put(key, response);
    }

    @Override
    public ResponseEntity<T> get(UUID key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void remove(UUID key) {
        cache.invalidate(key);
    }
}