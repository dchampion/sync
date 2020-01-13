package com.dchampion.http;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

/**
 * An interface for caching HTTP response entities ({@link ResponseEntity}) in instances
 * of the class {@link AsyncRequestHandler}.
 * 
 * @param <T> the type of the body of the response entity.
 */
interface ResponseCache<T> {
    
    /**
     * Put a {@link ResponseEntity} in the cache.
     * 
     * @param key a unique {@link UUID} that serves as the lookup key for the response entity.
     * @param response the response entity.
     */
    void put(UUID key, ResponseEntity<T> response);

    /**
     * Look up a {@link ResponseEntity} using a {@link UUID} as a key.
     *
     * @param key a unique {@link UUID} that serves as the lookup key for the response entity.
     * @return a {@link ResponseEntity} containing HTTP headers and, optionally, a body.
     */
    ResponseEntity<T> get(UUID key);

    /**
     * Remove a {@link ResponseEntity} corresponding to the supplied key from the cache.
     * 
     * @param key the key of the response entity to remove.
     */
    void remove(UUID key);
}