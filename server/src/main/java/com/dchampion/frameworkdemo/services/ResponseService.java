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

@Service
public class ResponseService<T> {

    @Autowired
    private ResponseRepository responseRepository;

    public void put(UUID key, ResponseEntity<T> responseEntity) {
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

    public ResponseEntity<T> get(UUID key) {
        Response response = getNewOrExisting(key);

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

    public void remove(UUID key) {
        Response response = getNewOrExisting(key);
        responseRepository.delete(response);
    }

    private Response getNewOrExisting(UUID key) {
        Response entity = new Response();
        entity.setUuid(key.toString());

        Example<Response> ex = Example.of(entity);
        List<Response> existing = responseRepository.findAll(ex);
        if (existing.size() == 1) {
            entity = existing.get(0);
        }
        return entity;
    }
}