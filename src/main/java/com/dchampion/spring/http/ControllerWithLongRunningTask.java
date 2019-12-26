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

/**
 * A contrived {@link RestController} class that demonstrates the usage of an {@link AsyncRequestHandler}
 * to manage the lifecycle of a long-running HTTP request from a browser client.
 */
@RestController
@RequestMapping("/long-call")
public class ControllerWithLongRunningTask {

    @Autowired
    private AsyncRequestHandler<List<String>> handler;

    @PostMapping("/task")
    public ResponseEntity<List<String>> task() {
        // Submit the task with a timeout of 10 minutes. This is a non-blocking call.
        return handler.submit(() -> longRunningTask(), TimeUnit.MINUTES, 10);
    }

    @GetMapping("/task/{id}")
    public ResponseEntity<List<String>> task(@PathVariable String id) {
        // Check the status of the task.
        return handler.poll(id);
    }

    // A long-running task.
    private List<String> longRunningTask() throws InterruptedException {
        Thread.sleep(30000);
        return Arrays.asList("Hello", "world");
    }
}