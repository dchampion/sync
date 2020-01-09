package com.dchampion.spring.http;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
    public ResponseEntity<List<String>> submit(@RequestBody(required=false) TimeParams params) {
        TimeUnit timeUnit = params != null ? TimeParams.parseTimeUnit(params.getTimeUnit()) : TimeUnit.MINUTES;
        int duration = params != null ? params.getDuration() : 10;

        // Submit the task with a timeout of 10 minutes. This is a non-blocking call.
        return handler.submit(() -> longRunningTask(), timeUnit, duration);
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

class TimeParams {
    private String timeUnit;
    private int duration;

    String getTimeUnit() {
        return timeUnit;
    }

    int getDuration() {
        return duration;
    }

    void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    void setDuration(int duration) {
        this.duration = duration;
    }

    static TimeUnit parseTimeUnit(String timeUnit) {
        return "SECONDS".equals(timeUnit.toUpperCase()) ? TimeUnit.SECONDS : TimeUnit.MINUTES;
    }

    @Override
    public String toString() {
        return "TimeUnit = " + timeUnit + " Duration = " + duration;
    }
}