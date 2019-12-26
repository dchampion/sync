package com.dchampion.spring.http;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Use an instance of this class to execute a long-running HTTP method
 * asynchronously, to report on the status of the method while it is executing,
 * and to return the results of the method when it is complete.
 * <p>
 * Use the {@link #submit(Callable, TimeUnit, int)} method to submit for asynchronous
 * execution a long-running task wrapped in a {@link Callable} instance. Then
 * use the {@link #poll(String)} method to check the status of the long-running
 * task at periodic intervals. If the long-running task has completed by the
 * time {@link #poll(String)} is called, then {@link #poll(String)} will return
 * a status of {@link TaskStatus#COMPLETE}, along with the results of the
 * long-running task. Otherwise {@link #poll(String)} will return a status of
 * {@link TaskStatus#PENDING}.
 * <p>
 * The cardinality of runtime instances of this class should be one instance per
 * long-running REST controller method. That is, when using this class, declare
 * a separate instance of it for each long-running HTTP method you wish to use
 * it with.
 * <p>
 * <b>Important:</b> By default, instances of this class that are suitable for
 * singleton processes will be instantiated by the Spring runtime. Such
 * instances are preferable in the singleton-process use case because they will be
 * very performant. However, if your process is designed to scale to more than a
 * single instance, say in a containerized runtime environment, then you must
 * use a version of this class suitable for multiple process instances. This
 * is done in two steps, as specified here:
 * <ol>
 * <li>Declare a property {@code async.response_cache.scope=shared} in your
 * {@code application.properties} file (note that by default this property is
 * {@code async.response_cache.scope=in-process}).</li>
 * <li>Assuming you are using an RDBMS for persistent data storage, create a
 * table in your database with the following definition:</li>
 * </ol>
 * 
 * <pre>
 * CREATE TABLE RESPONSE_CACHE
 * (
 *   UUID               CHAR(36 BYTE),
 *   HEADERS            VARCHAR2(512 BYTE),
 *   BODY               CLOB
 * )
 * </pre>
 * 
 * Below is an example usage of this class, inside a REST controller class containing
 * a call to a long-running method:
 * 
 * <pre>
 * &#64;Autowired
 * private AsyncRequestHandler&lt;List&lt;String&gt;&gt; handler;
 * 
 * &#64;PostMapping("/task")
 * public ResponseEntity&lt;List&lt;String&gt;&gt; task() {
 *     // Submit the task with a timeout of 10 minutes. This is a non-blocking call.
 *     return handler.submit(() -&gt; longRunningTask(), TimeUnit.MINUTES, 10);
 * }
 *
 * &#64;GetMapping("/task/{id}")
 * public ResponseEntity&lt;List&lt;String&gt;&gt; task(&#64;PathVariable String id) {
 *     // Check the status of the task.
 *     return handler.poll(id);
 * }
 *
 * // A long-running task.
 * private List&lt;String&gt; longRunningTask() {
 *     Thread.sleep(60000);
 *     return Arrays.asList("Hello", "world");
 * }
 * </pre>
 * 
 * Below is the HTTP client's perspective of the above implementation. The first call
 * submits the long-running task for asynchronous execution:
 * 
 * <pre>
 * Request:
 * http://localhost:8080/task
 *
 * Response:
 * HTTP response status: 202 (Accepted)
 *
 * HTTP response header values: Task-Status=submitted
 *                              Task-Id=cf645961-9b2c-4a60-b994-a9f093e5ac56
 *
 * HTTP body: null
 * </pre>
 * 
 * Subsequent calls poll the previously submitted long-running task for its
 * status, supplying the Task-Id returned by the first call to this method:
 * 
 * <pre>
 * Request:
 * http://localhost:8080/task/cf645961-9b2c-4a60-b994-a9f093e5ac56
 *
 * Response:
 * HTTP response status: 200 (OK) if Task-Status is "pending" or "complete"
 *                       400 (BAD_REQUEST) if Task-Status is "unsubmitted"
 *                       500 (INTERNAL_SERVER_ERROR) if Task-Status is "error" or "timedout"
 *
 * HTTP response header values: Task-Status=pending
 *                              Task-Status=complete
 *                              Task-Status=unsubmitted
 *                              Task-Status=error
 *                              Task-Status=timedout
 *
 * HTTP body: ["Hello","world"] if Task-Status is "complete"
 *            null if Task-Status is any other value.
 * </pre>
 * 
 * If {@code Task-Status=complete}, the body of the {@link ResponseEntity} returned by the
 * REST call will contain the results of the long-running task.
 *
 * @param <T> the body of the HTTP response returned by the long-running task
 *            executed by an instance of this class.
 */
@Component
public class AsyncRequestHandler<T> {

    /**
     * Task status indicators.
     */
    public enum TaskStatus {
        SUBMITTED("submitted"),
        PENDING("pending"),
        COMPLETE("complete"),
        UNSUBMITTED("unsubmitted"),
        ERROR("error"),
        TIMEDOUT("timedout");

        private final String status;

        TaskStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    private static final String TASK_STATUS_KEY = "Task-Status";

    private static final Logger log = LoggerFactory.getLogger(AsyncRequestHandler.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private ResponseCache<T> responseCache;

    /**
     * Submits a long-running task, wrapped in a {@link Callable} instance, for
     * asynchronous execution from a REST controller method. This method returns
     * immediately; that is, it does not wait for the task to complete.
     * <p>
     * See {@link AsyncRequestHandler class-level Javadoc} for an example usage of this
     * method.
     *
     * @param task     The {@link Callable} instance containing the long-running task.
     *                 The type parameter of the {@link Callable} instance, and its return
     *                 value, will comprise the contents of the HTTP response body when the
     *                 task is complete.
     * @param timeUnit The {@link TimeUnit} to use for timeout; e.g. {@link TimeUnit#MINUTES}.
     * @param timeout  The time allowed for the long-running task to complete. If the task
     *                 does not complete within this duration, this class' {@link #poll(String)}
     *                 method will report a status of {@link TaskStatus#TIMEDOUT} in its header's
     *                 {@code Task-Status} field. This value must be a positive integer.
     *
     * @return a {@link ResponseEntity} containing no body, but with a header
     *         containing a {@code Task-Status} of {@link TaskStatus#SUBMITTED}, and a
     *         {@code Task-Id} to be used in subsequent calls to this class'
     *         {@link #poll(String)} method. This {@link ResponseEntity} should be
     *         returned immediately to the client making the long-running REST call.
     *         An HTTP status of 202 (Accepted) is included in the response to
     *         further indicate to the client that the request has been received but
     *         not yet fulfilled.
     *
     * @throws NullPointerException     if either arg {@code task} or {@code timeUnit} is {@code null}.
     * @throws IllegalArgumentException if arg {@code timeout} is not a positive integer.
     */
    public ResponseEntity<T> submit(Callable<T> task, TimeUnit timeUnit, int timeout) {

        // Validate args.
        Objects.requireNonNull(task);
        Objects.requireNonNull(timeUnit);
        if (timeout < 1) {
            throw new IllegalArgumentException("Timeout must be a positive integer.");
        }

        // Generate unique task ID for task tracking.
        UUID uuid = UUID.randomUUID();
        
        // Build an initial response to return immediately to the caller.
        ResponseEntity<T> response = new ResponseBuilder<T>()
                .header("Task-Id", uuid.toString())
                .header(TASK_STATUS_KEY, TaskStatus.SUBMITTED)
                .status(HttpStatus.ACCEPTED).build();
        
        // Cache it.
        responseCache.put(uuid, response);

        // Submit the task for asynchronous execution (this call doesn't block).
        executorService.execute(() -> doTask(uuid, task, timeUnit, timeout));

        // Return the response.
        return response;
    }

    /**
     * Executes long-running task and waits for it to finish.
     * 
     * @param uuid A unique task ID.
     * @param task The task.
     * @param timeUnit The timeout parameter's {@link TimeUnit}.
     * @param timeout The timeout value for the task.
     */
    private void doTask(UUID uuid, Callable<T> task, TimeUnit timeUnit, int timeout) {
        try {
            // Execute the task and wait for it to finish.
            T body = executorService.submit(task).get(timeout, timeUnit);
            
            // Build a response containing the results of the task.
            ResponseEntity<T> response = new ResponseBuilder<T>()
                    .header(TASK_STATUS_KEY, TaskStatus.COMPLETE)
                    .body(body)
                    .status(HttpStatus.OK)
                    .build();
            
            // Cache it.
            responseCache.put(uuid, response);

        } catch (InterruptedException e) {
            log.warn("Task for id " + uuid.toString() + " interrupted.", e);
            buildErrorResponse(uuid, TaskStatus.ERROR, e);
            Thread.currentThread().interrupt();

        } catch (ExecutionException e) {
            log.error("Task for id " + uuid.toString() + " threw an exception.", e);
            buildErrorResponse(uuid, TaskStatus.ERROR, e);

        } catch (TimeoutException e) {
            log.warn("Task for id " + uuid.toString() + " timed out.");
            buildErrorResponse(uuid, TaskStatus.TIMEDOUT, e);
        }
    }

    /**
     * {@link #doTask(UUID, Callable, TimeUnit, int)} helper method to build up an error response.
     * 
     * @param uuid The task ID.
     * @param status The {@link #TaskStatus}.
     * @param t The {@link Throwable} thrown by the task.
     */
    private void buildErrorResponse(UUID uuid, TaskStatus status, Throwable t) {
        
        // Get the original cause if ExecutionException.
        if (t instanceof ExecutionException) {
            t = t.getCause();
        }
        
        // Build a response.
        ResponseEntity<T> response = new ResponseBuilder<T>()
                .header(TASK_STATUS_KEY, status)
                .header("Task-Error-Type", t.getClass() != null ? t.getClass().getName() : "Unknown")
                .header("Task-Error-Message", t.getMessage() != null ? t.getMessage() : "None")
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        
        // Cache it.
        responseCache.put(uuid, response);
    }

    /**
     * Polls the status of a long-running task previously submitted to this class'
     * {@link #submit(Callable, TimeUnit, int)} method, given the {@code Task-Id}.
     * <p>
     * One of five possible statuses is returned in the header of the
     * {@link ResponseEntity} returned by this method, depending on the state of the
     * long-running task:
     * <ol>
     * <li>{@code Task-Status}={@link TaskStatus#UNSUBMITTED}: No task was found for the
     * supplied id; either no such task was ever submitted, or the task has since
     * completed.</li>
     * <li>{@code Task-Status}={@link TaskStatus#PENDING}: The task is currently
     * running.</li>
     * <li>{@code Task-Status}={@link TaskStatus#COMPLETE}: The task is complete.</li>
     * <li>{@code Task-Status}={@link TaskStatus#ERROR}: The task threw an exception, or was
     * interrupted by another thread.</li>
     * <li>{@code Task-Status}={@link TaskStatus#TIMEDOUT}: The task did not complete before
     * the timeout value specified in the submit() call expired.</li>
     * </ol>
     * If the {@code Task-Status} is {@link TaskStatus#COMPLETE}, then the results of the
     * long-running task will be included in the body of the {@link ResponseEntity}
     * returned by this method. In all other cases, the body will be {@code null}.
     * <p>
     * If {@code Task-Status} is {@link TaskStatus#ERROR}, then two additional headers
     * will be included in the response indicating the type of error
     * ("Task-Error-Type") and an error message ("Task-Error-Message").
     * <p>
     * See {@link AsyncRequestHandler class-level Javadoc} for sample usages of this
     * method.
     *
     * @param id the id of the long-running task. This id is supplied in the
     *           {@code Task-Id} header of the {@link ResponseEntity} returned by this
     *           class' {@link #submit(Callable, TimeUnit, int)} method.
     *
     * @return a {@link ResponseEntity} containing a {@code Task-Status} header indicating
     *         the status of the long-running task and, if the status is
     *         {@link TaskStatus#COMPLETE}, the results of the long-running task in its
     *         body.
     *
     * @throws NullPointerException if arg {@code id} is {@code null}.
     */
    public ResponseEntity<T> poll(String id) {

        Objects.requireNonNull(id);

        // Get the current state of the task from the response cache.
        UUID uuid = UUID.fromString(id);
        ResponseEntity<T> response = responseCache.get(uuid);
        
        if (response == null) {
            
            // We have no response for the supplied task ID.
            response = new ResponseBuilder<T>()
                .header(TASK_STATUS_KEY, TaskStatus.UNSUBMITTED)
                .status(HttpStatus.BAD_REQUEST)
                .build();

        } else {
            
            // If we have a response body we know the task has finished.
            T body = response.getBody();
            if (body != null) {

                // Remove the response from the cache as we are done.
                responseCache.remove(uuid);
                
                // Build up a response containing the results of the task.
                response = new ResponseBuilder<T>()
                    .header(TASK_STATUS_KEY, TaskStatus.COMPLETE)
                    .body(body)
                    .status(HttpStatus.OK)
                    .build();

            } else {
                
                // Task has not finished; adjust headers accordingly.
                HttpHeaders headers = response.getHeaders();
                String status = headers.getFirst(TASK_STATUS_KEY);
                if (status.equals(TaskStatus.SUBMITTED.getStatus())) {
                    
                    // This is the first time we've been polled; tell the caller
                    // the task is still pending.
                    response = new ResponseBuilder<T>()
                        .header(TASK_STATUS_KEY, TaskStatus.PENDING)
                        .status(HttpStatus.OK)
                        .build();
                    
                    responseCache.put(uuid, response);
                } else if (status.equals(TaskStatus.ERROR.getStatus())
                        || status.equals(TaskStatus.TIMEDOUT.getStatus())) {
                    
                    // In the case of an error or a timeout, remove the
                    // response from the cache.
                    responseCache.remove(uuid);
                }
            }
        }

        return response;
    }
}

/**
 * A convenience class for building up an HTTP {@link ResponseEntity}.
 * 
 * @param <T> the type of the {@link ResponseEntity}'s body.
 */
final class ResponseBuilder<T> {

    private HttpHeaders headers = new HttpHeaders();
    private T body = null;
    private HttpStatus status;

    ResponseBuilder() {
    }

    /**
     * Set a single header pair. This is a convenience method specific to Task-Status headers.
     * 
     * @param headerName the name of the header (i.e. Task-Status).
     * @param status the {@link #TaskStatus}.
     * 
     * @return an instance of this class containing the supplied header info.
     */
    ResponseBuilder<T> header(String headerName, AsyncRequestHandler.TaskStatus status) {
        return header(headerName, status.getStatus());
    }

    /**
     * Set a single header pair.
     * 
     * @param headerName the name of the header.
     * @param headerValue the value of the header.
     * 
     * @return an instance of this class containing the supplied header info.
     */
    ResponseBuilder<T> header(String headerName, String headerValue) {
        headers.set(headerName, headerValue);
        return this;
    }

    /**
     * Set a body.
     * 
     * @param body The response body.
     * 
     * @return an instance of this class containing the supplied body.
     */
    ResponseBuilder<T> body(T body) {
        this.body = body;
        return this;
    }

    /**
     * Set an HTTP status.
     * 
     * @param status the {@link HttpStatus}.
     * 
     * @return an instance of this class containing the supplied HTTP status.
     */
    ResponseBuilder<T> status(HttpStatus status) {
        this.status = status;
        return this;
    }

    /**
     * The terminal operation of this class, which returns a {@link ResponseEntity} containing
     * zero or more headers, an optional body, and an HTTP status supplied via intermediate operations of
     * this class. Note that at a minimum an HTTP status must be supplied.
     * 
     * @return a valid {@link ResponseEntity}.
     */
    ResponseEntity<T> build() {
        assert (status != null);
        ResponseEntity<T> response = new ResponseEntity<T>(body, headers, status);
        return response;
    }
}
