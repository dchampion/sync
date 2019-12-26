package com.dchampion.spring.http;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/long-call")
public class Controller {

    @Autowired
    private AsyncRequestHandler<List<String>> handler;

    @PostMapping("/job")
    public ResponseEntity<List<String>> job() {
        return handler.submit(() -> longRunningJob(), TimeUnit.MINUTES, 20);
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<List<String>> job(@PathVariable String id) {
        return handler.poll(id);
    }

    private List<String> longRunningJob() throws InterruptedException {
        Thread.sleep(30000);
        return Arrays.asList("Hello", "world");
    }
}