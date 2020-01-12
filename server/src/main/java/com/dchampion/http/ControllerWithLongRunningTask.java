package com.dchampion.http;

import java.util.List;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/submit")
    public ResponseEntity<List<String>> submit(@RequestBody(required=false) TimeoutParameter param) {
        int timeout = param != null ? param.getTimeout() : 10;

        // Submit the task with a timeout of 10 minutes. This is a non-blocking call.
        return handler.submit(() -> longRunningTask(), timeout);
    }

    @GetMapping("/poll/{id}")
    public ResponseEntity<List<String>> poll(@PathVariable String id) {
        // Check the status of the task.
        return handler.poll(id);
    }

    // A long-running task.
    private List<String> longRunningTask() throws InterruptedException {
        Thread.sleep(9000);
        return Arrays.asList("Hello", "Client!");
    }
}

class TimeoutParameter {
    private int timeout;

    int getTimeout() {
        return timeout;
    }

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "Timeout: " + timeout;
    }
}