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
 * a status of {@link JobStatus#COMPLETE}, along with the results of the
 * long-running task. Otherwise {@link #poll(String)} will return a status of
 * {@link JobStatus#PENDING}.
 * <p>
 * The cardinality of runtime instances of this class should be one instance per
 * long-running REST controller method. That is, when using this class, declare
 * a separate instance of it for each long-running HTTP method you wish to use
 * it with. This implies that if you have a Spring {@code @RestController}-annotated
 * class containing a single long-running REST controller method, then declare
 * one instance of this class as a member variable of the
 * {@code @RestController}-annotated class.
 * <p>
 * <b>Important:</b> By default, instances of this class that are suitable for
 * singleton processes will be instantiated by the Spring runtime. Such
 * instances are preferable in the singleton-process use case because they are
 * very performant. However, if your process is designed to scale to more than a
 * single instance, say in a containerized runtime environment, then you must
 * use a version of this class suitable for multiple process instances. This
 * is done in two steps, as specified here:
 * <ol>
 * <li>Declare a property {@code async.response_cache.scope=shared} in your
 * {@code application.properties} file (note by default this property is
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
 * Below is an example usage of this class in a REST controller class containing
 * a call to a long-running method:
 * 
 * <pre>
 * &#64;Autowired
 * private AsyncRequestHandler&lt;List&lt;String&gt;&gt; handler;
 * 
 * &#64;PostMapping("/job")
 * public ResponseEntity&lt;List&lt;String&gt;&gt; job() {
 *     return handler.submit(() -&gt; longRunningJob(), TimeUnit.MINUTES, 10);
 * }
 *
 * &#64;GetMapping("/job/{id}")
 * public ResponseEntity&lt;List&lt;String&gt;&gt; job(&#64;PathVariable String id) {
 *     return handler.poll(id);
 * }
 *
 * // A long-running job.
 * private List&lt;String&gt; longRunningJob() {
 *     Thread.sleep(10000);
 *     return Arrays.asList("Hello, ", "world");
 * }
 * </pre>
 * 
 * Below is the HTTP client's perspective of the above implementation. The first call
 * submits the long-running task for asynchronous execution:
 * 
 * <pre>
 * Request:
 * http://localhost:8080/job
 *
 * Response:
 * HTTP response status: 202 (Accepted)
 *
 * HTTP response header values: Job-Status=submitted
 *                              Job-Id=cf645961-9b2c-4a60-b994-a9f093e5ac56
 *
 * HTTP body: null
 * </pre>
 * 
 * Subsequent calls poll the previously submitted long-running task for its
 * status, supplying the Job-Id returned by the first, parameterless call to
 * this method:
 * 
 * <pre>
 * Request:
 * http://localhost:8080/job/cf645961-9b2c-4a60-b994-a9f093e5ac56
 *
 * Response:
 * HTTP response status: 200 (OK), if Job-Status is "pending" or "complete"
 *                       400 (BAD_REQUEST), if Job-Status is "unsubmitted"
 *                       500 (INTERNAL_SERVER_ERROR), if Job-Status is "error" or "timedout"
 *
 * HTTP response header values: Job-Status=pending
 *                              Job-Status=complete
 *                              Job-Status=unsubmitted
 *                              Job-Status=error
 *                              Job-Status=timedout
 *
 * HTTP body: ["Hello, ","world"], if Job-Status is "complete"
 *            null, if Job-Status is any other value.
 * </pre>
 * 
 * If Job-Status=complete, the body of the {@link ResponseEntity} returned by the
 * REST call will contain the results of the long-running task.
 *
 * @param <T> the body of the HTTP response returned by the long-running task
 *            executed by an instance of this class.
 */
@Component
public class AsyncRequestHandler<T> {

    /**
     * Job status indicators.
     */
    public enum JobStatus {
        SUBMITTED("submitted"),
        PENDING("pending"),
        COMPLETE("complete"),
        UNSUBMITTED("unsubmitted"),
        ERROR("error"),
        TIMEDOUT("timedout");

        private final String status;

        JobStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    private static final String JOB_STATUS_KEY = "Job-Status";

    private static final Logger log = LoggerFactory.getLogger(AsyncRequestHandler.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private ResponseCache<T> responseCache;

    /**
     * Submits a long-running task, wrapped in a {@link Callable} instance, for
     * asynchronous execution inside a REST controller method. This method returns
     * immediately; that is, it does not wait for the task to complete.
     * <p>
     * See {@link AsyncRequestHandler class-level Javadoc} for an example usage of this
     * method.
     *
     * @param job      The {@link Callable} instance containing the long-running task.
     *                 The type parameter of the {@link Callable} instance, and its return
     *                 value, will comprise the contents of the HTTP response body when the
     *                 task is complete.
     * @param timeUnit The {@link TimeUnit} to use for timeout; e.g. {@link TimeUnit#MINUTES}.
     * @param timeout  The time allowed for the long-running task to complete. If the task
     *                 does not complete within this duration, this class' {@link #poll(String)}
     *                 method will report a status of {@link JobStatus#TIMEDOUT} in its header's
     *                 {@code Job-Status} field. This value must be a positive integer.
     *
     * @return a {@link ResponseEntity} containing no body, but with a header
     *         containing a {@code Job-Status} of {@link JobStatus#SUBMITTED}, and a
     *         {@code Job-Id} to be used in subsequent calls to this class'
     *         {@link #poll(String)} method. This {@link ResponseEntity} should be
     *         returned immediately to the client making the long-running REST call.
     *         An HTTP status of 202 (Accepted) is included in the response to
     *         further indicate to the client that the request has been received but
     *         not yet fulfilled.
     *
     * @throws NullPointerException     if arg {@code task} is {@code null}.
     * @throws IllegalArgumentException if arg {@code timeout} is not a positive integer.
     */
    public ResponseEntity<T> submit(Callable<T> job, TimeUnit timeUnit, int timeout) {

        // Validate args.
        Objects.requireNonNull(job);
        Objects.requireNonNull(timeUnit);
        if (timeout < 1) {
            throw new IllegalArgumentException("Timeout must be a positive integer.");
        }

        // Generate unique job ID for job tracking.
        UUID uuid = UUID.randomUUID();
        
        // Build an initial response to return immediately to the caller.
        ResponseEntity<T> response = new ResponseBuilder<T>()
                .header("Job-Id", uuid.toString())
                .header(JOB_STATUS_KEY, JobStatus.SUBMITTED)
                .status(HttpStatus.ACCEPTED).build();
        
        // Cache it.
        responseCache.put(uuid, response);

        // Submit the job for asynchronous execution (this call doesn't block).
        executorService.execute(() -> doJob(uuid, job, timeUnit, timeout));

        // Return the response.
        return response;
    }

    /**
     * Executes long-running job and waits for it to finish.
     * 
     * @param uuid A unique job ID.
     * @param job The job.
     * @param timeUnit The timeout parameter's {@link TimeUnit}.
     * @param timeout The timeout value for the job.
     */
    private void doJob(UUID uuid, Callable<T> job, TimeUnit timeUnit, int timeout) {
        try {
            // Execute the job and wait for it to finish.
            T body = executorService.submit(job).get(timeout, timeUnit);
            
            // Build a response containing the results of the job.
            ResponseEntity<T> response = new ResponseBuilder<T>()
                    .header(JOB_STATUS_KEY, JobStatus.COMPLETE)
                    .body(body)
                    .status(HttpStatus.OK)
                    .build();
            
            // Cache it.
            responseCache.put(uuid, response);

        } catch (InterruptedException e) {
            log.warn("Task for id " + uuid.toString() + " interrupted.", e);
            buildErrorResponse(uuid, JobStatus.ERROR, e);
            Thread.currentThread().interrupt();

        } catch (ExecutionException e) {
            log.error("Task for id " + uuid.toString() + " threw an exception.", e);
            buildErrorResponse(uuid, JobStatus.ERROR, e);

        } catch (TimeoutException e) {
            log.warn("Task for id " + uuid.toString() + " timed out.");
            buildErrorResponse(uuid, JobStatus.TIMEDOUT, e);
        }
    }

    /**
     * {@link #doJob(UUID, Callable, TimeUnit, int)} helper method to build up an error response.
     * 
     * @param uuid The Job ID.
     * @param status The {@link #JobStatus}.
     * @param t The {@link Throwable} thrown by the job task.
     */
    private void buildErrorResponse(UUID uuid, JobStatus status, Throwable t) {
        
        // Get the original cause if ExecutionException.
        if (t instanceof ExecutionException) {
            t = t.getCause();
        }
        
        // Build a response.
        ResponseEntity<T> response = new ResponseBuilder<T>()
                .header(JOB_STATUS_KEY, status)
                .header("Job-Error-Type", t.getClass() != null ? t.getClass().getName() : "Unknown")
                .header("Job-Error-Message", t.getMessage() != null ? t.getMessage() : "None")
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        
        // Cache it.
        responseCache.put(uuid, response);
    }

    /**
     * Polls the status of a long-running task previously submitted to this class'
     * {@link #submit(Callable, TimeUnit, int)} method, given the {@code Job-Id}.
     * <p>
     * One of five possible statuses is returned in the header of the
     * {@link ResponseEntity} returned by this method, depending on the state of the
     * long-running job:
     * <ol>
     * <li>{@code Job-Status}={@link JobStatus#UNSUBMITTED}: No task was found for the
     * supplied id; either no such task was ever submitted, or the task has since
     * completed.</li>
     * <li>{@code Job-Status}={@link JobStatus#PENDING}: The task is currently
     * running.</li>
     * <li>{@code Job-Status}={@link JobStatus#COMPLETE}: The task is complete.</li>
     * <li>{@code Job-Status}={@link JobStatus#ERROR}: The task threw an exception, or was
     * interrupted by another thread.</li>
     * <li>{@code Job-Status}={@link JobStatus#TIMEDOUT}: The task did not complete before
     * the timeout specified in the submit() call expired.</li>
     * </ol>
     * If the {@code Job-Status} is {@link JobStatus#COMPLETE}, then the results of the
     * long-running task will be included in the body of the {@link ResponseEntity}
     * returned by this method. In all other cases, the body will be {@code null}.
     * <p>
     * If {@code Job-Status} is {@link JobStatus#ERROR}, then two additional headers
     * will be included in the response indicating the type of error
     * ("Job-Error-Type") and an error message ("Job-Error-Message").
     * <p>
     * See {@link AsyncRequestHandler class-level Javadoc} for sample usages of this
     * method.
     *
     * @param id the id of the long-running job. This id is supplied in the
     *           {@code Job-Id} header of the {@link ResponseEntity} returned by this
     *           class' {@link #submit(Callable, TimeUnit, int)} method.
     *
     * @return a {@link ResponseEntity} containing a {@code Job-Status} header indicating
     *         the status of the long-running job and, if the status is
     *         {@link JobStatus#COMPLETE}, the results of the long-running job in its
     *         body.
     *
     * @throws NullPointerException if arg {@code id} is {@code null}.
     */
    public ResponseEntity<T> poll(String id) {

        Objects.requireNonNull(id);

        // Get the current state of the job from the response cache.
        UUID uuid = UUID.fromString(id);
        ResponseEntity<T> response = responseCache.get(uuid);
        
        if (response == null) {
            
            // We have no response for the supplied job ID.
            response = new ResponseBuilder<T>()
                .header(JOB_STATUS_KEY, JobStatus.UNSUBMITTED)
                .status(HttpStatus.BAD_REQUEST)
                .build();

        } else {
            
            // If we have a response body we know the job has finished.
            T body = response.getBody();
            if (body != null) {

                // Remove the response from the cache as we are done.
                responseCache.remove(uuid);
                
                // Build up a response containing the results of the job.
                response = new ResponseBuilder<T>()
                    .header(JOB_STATUS_KEY, JobStatus.COMPLETE)
                    .body(body)
                    .status(HttpStatus.OK)
                    .build();

            } else {
                
                // Job has not finished; adjust headers accordingly.
                HttpHeaders headers = response.getHeaders();
                String status = headers.getFirst(JOB_STATUS_KEY);
                if (status.equals(JobStatus.SUBMITTED.getStatus())) {
                    
                    // This is the first time we've been polled; tell the caller
                    // the job is still pending.
                    response = new ResponseBuilder<T>()
                        .header(JOB_STATUS_KEY, JobStatus.PENDING)
                        .status(HttpStatus.OK)
                        .build();
                    
                    responseCache.put(uuid, response);
                } else if (status.equals(JobStatus.ERROR.getStatus())
                        || status.equals(JobStatus.TIMEDOUT.getStatus())) {
                    
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
     * Set a single header pair. This is a convenience method specific to job-status headers.
     * 
     * @param headerName the name of the header (i.e. Job-Status).
     * @param status the {@link #JobStatus}.
     * 
     * @return an instance of this class containing the supplied header info.
     */
    ResponseBuilder<T> header(String headerName, AsyncRequestHandler.JobStatus status) {
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
