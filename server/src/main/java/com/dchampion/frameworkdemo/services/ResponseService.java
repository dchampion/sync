package com.dchampion.frameworkdemo.services;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.dchampion.frameworkdemo.entities.Response;
import com.dchampion.frameworkdemo.repositories.ResponseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * A service that fronts a persistent data store for {@link Response} entities.
 */
@Service
public class ResponseService {

    @Autowired
    private ResponseRepository responseRepository;

    /**
     * Either saves a new {@link ResponseEntity} in the data store, or updates
     * an existing one if it is aready present in the data store.
     *
     * @param <T> The type of the {@link ResponseEntity}'s body.
     * @param key The key by which to store a {@link ResponseEntity}.
     * @param responseEntity The {@link ResponseEntity} to store.
     */
    public <T> void saveOrUpdate(UUID key, ResponseEntity<T> responseEntity) {
        Response response = getNewOrExisting(key);

        StringBuilder headerBuilder = new StringBuilder();
        HttpHeaders headers = responseEntity.getHeaders();
        headers.forEach((hKey, list) -> {
            if (hKey.startsWith("Task-")) {
                headerBuilder.append(hKey).append("=").append(list.get(0)).append(":");
            }
        });
        headerBuilder.deleteCharAt(headerBuilder.length() - 1);
        response.setHeaders(headerBuilder.toString());

        response.setBody(responseEntity.getBody());

        responseRepository.save(response);
    }

    /**
     * Retrieves the {@link ResponseEntity} corresponding to the given key from
     * the data store, or {@code null} if no such entity exists in the data
     * store.
     *
     * @param <T> The type of the {@link ResponseEntity}'s body.
     * @param key The key by which to retrieve a {@link ResponseEntity}.
     *
     * @return A {@link ResponseEntity} corresponding to the given key, or
     * {@code null} if no such entity exists.
     */
    public <T> ResponseEntity<T> getIfPresent(UUID key) {
        Response response = getNewOrExisting(key);

        // A Response entity, if present in the database, must
        // have headers; if not, the caller expects a null return value.
        if (response.getHeaders() == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        List<String> asList = Arrays.asList(response.getHeaders().split(":"));
        asList.forEach(header -> {
            headers.set(header.split("=")[0], header.split("=")[1]);
        });

        @SuppressWarnings("unchecked")
        ResponseEntity<T> responseEntity =
            new ResponseEntity<T>((T)response.getBody(), headers, HttpStatus.OK);

        return responseEntity;
    }

    /**
     * Deletes the {@link ResponseEntity} corresponding to the given key from
     * the data store.
     *
     * @param key The key corresponding to the {@link ResponseEntity}.
     */
    public void remove(UUID key) {
        Response response = getNewOrExisting(key);
        responseRepository.delete(response);
    }

    private Response getNewOrExisting(UUID key) {
        Response entity = new Response();
        entity.setUuid(key.toString());

        Example<Response> example = Example.of(entity);
        List<Response> existing = responseRepository.findAll(example);

        return existing.size() == 1 ? existing.get(0) : entity;
    }
}
