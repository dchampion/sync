package com.dchampion.spring;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * A shared implementation of {@link ResponseCache}. This implementation
 * will be used if the property {@code async.response_cache.scope=shared} is set.
 * <p>
 * See {@link AsyncRequestHandler} for hints on how one might implement this class.
 * 
 * @param <T> the type of the body of a {@link ResponseEntity}.
 * 
 * @see InProcessResponseCache
 */
@Component
@ConditionalOnProperty(name = "async.response_cache.scope", havingValue = "shared")
public class SharedResponseCache<T> implements ResponseCache<T> {

    @Override
    public void put(UUID key, ResponseEntity<T> response) {
        // TODO: Implement me.
    }

    @Override
    public ResponseEntity<T> get(UUID key) {
        // TODO: Implement me.
        return new ResponseEntity<T>(HttpStatus.I_AM_A_TEAPOT);
    }

    @Override
    public void remove(UUID key) {
        // TODO: Implement me.
    }
}