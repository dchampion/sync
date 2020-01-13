package com.dchampion.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AsyncRequestHandlerTest {
    private AsyncRequestHandler<List<String>> handler;

    @BeforeEach
    public void setup() {
        handler = new AsyncRequestHandler<>();
        handler.responseCache = new InProcessResponseCache<List<String>>();
    }

    private void assertSubmitted(ResponseEntity<List<String>> response) {
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(AsyncRequestHandler.TaskStatus.SUBMITTED.getStatus(), response.getHeaders().getFirst("Task-Status"));
    }

    private void assertPending(ResponseEntity<List<String>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AsyncRequestHandler.TaskStatus.PENDING.getStatus(), response.getHeaders().getFirst("Task-Status"));
    }

    private void assertUnsubmitted(ResponseEntity<List<String>> response) {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(AsyncRequestHandler.TaskStatus.UNSUBMITTED.getStatus(), response.getHeaders().getFirst("Task-Status"));
    }

    private void assertComplete(ResponseEntity<List<String>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(response.getHeaders().getFirst("Task-Status"), AsyncRequestHandler.TaskStatus.COMPLETE.getStatus());
        assertEquals(response.getBody().size(), 3);
    }

    private void assertError(ResponseEntity<List<String>> response) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(AsyncRequestHandler.TaskStatus.ERROR.getStatus(), response.getHeaders().getFirst("Task-Status"));
        assertFalse(response.getHeaders().getFirst("Task-Error-Type").isEmpty());
        assertFalse(response.getHeaders().getFirst("Task-Error-Message").isEmpty());
    }

    private void assertTimedout(ResponseEntity<List<String>> response) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(AsyncRequestHandler.TaskStatus.TIMEDOUT.getStatus(), response.getHeaders().getFirst("Task-Status"));
        assertFalse(response.getHeaders().getFirst("Task-Error-Type").isEmpty());
        assertFalse(response.getHeaders().getFirst("Task-Error-Message").isEmpty());
    }

    private List<String> longRunningTask() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail("longRunningTask() interrupted!");
        }
        return Arrays.asList("1", "2", "3");
    }

    private List<String> exceptionThrowingLongRunnningTask() {
        longRunningTask();
        throw new RuntimeException("I'm a RuntimeException!");
    }

    @Test
    public void submitAndPoll() {

        ResponseEntity<List<String>> response = handler.submit(() -> longRunningTask(), 10);
        assertSubmitted(response);

        String taskId = response.getHeaders().getFirst("Task-Id");

        response = handler.poll(UUID.randomUUID().toString());
        assertUnsubmitted(response);

        response = handler.poll(taskId);
        while (!AsyncRequestHandler.TaskStatus.COMPLETE.getStatus().equals(response.getHeaders().getFirst("Task-Status"))) {
            assertPending(response);
            response = handler.poll(taskId);
        }
        assertComplete(response);

        response = handler.poll(taskId);
        assertUnsubmitted(response);
    }

    @Test
    public void submitAndWaitTillComplete() throws InterruptedException {

        ResponseEntity<List<String>> response = handler.submit(() -> longRunningTask(), 10);
        assertSubmitted(response);

        String taskId = response.getHeaders().getFirst("Task-Id");

        Thread.sleep(6000);

        response = handler.poll(taskId);
        assertComplete(response);

        response = handler.poll(taskId);
        assertUnsubmitted(response);
    }

    @Test
    public void submitAndPollExceptionThrowingTask() {

        ResponseEntity<List<String>> response = handler.submit(() -> exceptionThrowingLongRunnningTask(), 10);
        assertSubmitted(response);

        String taskId = response.getHeaders().getFirst("Task-Id");

        response = handler.poll(taskId);
        assertPending(response);
        while (AsyncRequestHandler.TaskStatus.PENDING.getStatus().equals(response.getHeaders().getFirst("Task-Status"))) {
            assertPending(response);
            response = handler.poll(taskId);
        }

        assertError(response);

        response = handler.poll(taskId);
        assertUnsubmitted(response);
    }

    @Test
    public void submitAndPollWithTimeout() {

        ResponseEntity<List<String>> response = handler.submit(() -> longRunningTask(), 2);
        assertSubmitted(response);

        String taskId = response.getHeaders().getFirst("Task-Id");
        response = handler.poll(taskId);
        assertPending(response);
        while (AsyncRequestHandler.TaskStatus.PENDING.getStatus().equals(response.getHeaders().getFirst("Task-Status"))) {
            assertPending(response);
            response = handler.poll(taskId);
        }

        assertTimedout(response);
        response = handler.poll(taskId);

        assertUnsubmitted(response);
    }

    @Test
    public void volume() throws InterruptedException {

        // Submit for async execution 5 long-running (i.e. 5 seconds each) tasks,
        // more or less simultaneously, in the current thread.
        List<String> taskIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String id = handler.submit(() -> longRunningTask(), 30).getHeaders().getFirst("Task-Id");
            taskIds.add(id);
        }

        // Poll for each task submitted above in a dedicated thread (i.e. poll in parallel).
        List<String> resultStatuses = new CopyOnWriteArrayList<>();
        Executor executor = Executors.newFixedThreadPool(5);
        taskIds.forEach(taskId -> {
            executor.execute(() -> {
                HttpHeaders headers = handler.poll(taskId).getHeaders();
                String status = headers.getFirst("Task-Status");
                while (!status.equals(AsyncRequestHandler.TaskStatus.COMPLETE.getStatus())) {
                    headers = handler.poll(taskId).getHeaders();
                    status = headers.getFirst("Task-Status");
                }
                assertEquals(AsyncRequestHandler.TaskStatus.COMPLETE.getStatus(), status);
                resultStatuses.add(status);
            });
        });

        // Sleep for n+1 seconds, where n is the the time required to execute the task
        // submitted above (i.e. longRunningTask()) once.
        Thread.sleep(6000);

        // The cached thread pool implementation in AsyncRequestHandler is backed by a
        // SynchronousQueue. Therefore new threads should always be created on-demand,
        // if none is already available in the pool, and should never wait in a queue
        // before being executed. These properties guarantee that all 5 long-running
        // tasks should run immediately and in parallel.
        assertEquals(5, resultStatuses.size());
    }
}
