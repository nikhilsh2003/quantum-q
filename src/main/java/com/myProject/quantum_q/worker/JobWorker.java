package com.myProject.quantum_q.worker;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

@Service
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final List<String> QUEUES = List.of("queues:high", "queues:normal", "queues:low");
    private static final String PROCESSING_QUEUE = "queues:processing";
    private static final String RETRY_SCHEDULE = "schedule:retries";
    private static final String DLQ = "queues:dlq";
    private static final int MAX_RETRIES = 5;

    private final RedisTemplate<String, String> redisTemplate;

    public JobWorker(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void start() {
        // Use a virtual thread for the long-running, blocking worker loop
        Thread.ofVirtual().start(this::workerLoop);
    }

    private void workerLoop() {
        log.info("Worker started. Polling for jobs from {}...", QUEUES);
        while (true) {
            String jobId = findNextJob();
            if (jobId != null) {
                processJob(jobId);
            }
        }
    }

    private String findNextJob() {
        for (String queue : QUEUES) {
            String jobId = redisTemplate.opsForList().rightPopAndLeftPush(queue, PROCESSING_QUEUE, 1, TimeUnit.SECONDS);
            if (jobId != null) {
                return jobId;
            }
        }
        return null;
    }

    private void processJob(String jobId) {
        String jobKey = "job:" + jobId;
        try {
            redisTemplate.opsForHash().put(jobKey, "status", "PROCESSING");
            log.info("Processing job: {}", jobId);

            // Simulate work
            sleep(1500);

            redisTemplate.opsForHash().put(jobKey, "status", "COMPLETED");
            redisTemplate.opsForList().remove(PROCESSING_QUEUE, 1, jobId);
            log.info("✅ Job {} completed successfully.", jobId);

        } catch (Exception e) {
            log.error("🔥 Failed to process job {}. Initiating failure protocol.", jobId, e);
            handleFailedJob(jobId, jobKey);
        }
    }

    private void handleFailedJob(String jobId, String jobKey) {
        try {
            long retryCount = redisTemplate.opsForHash().increment(jobKey, "retryCount", 1);

            if (retryCount < MAX_RETRIES) {
                log.warn("Scheduling retry #{} for job {}", retryCount, jobId);
                double retryTime = System.currentTimeMillis() + (2000 * Math.pow(2, retryCount)); // Exponential backoff
                redisTemplate.opsForZSet().add(RETRY_SCHEDULE, jobId, retryTime);
            } else {
                log.error("Job {} failed after {} retries. Moving to DLQ.", jobId, MAX_RETRIES);
                redisTemplate.opsForHash().put(jobKey, "status", "FAILED");
                String jobPayload = (String) redisTemplate.opsForHash().get(jobKey, "payload");
                redisTemplate.opsForList().leftPush(DLQ, jobPayload);
            }
        } finally {
            // Ensure job is always removed from the processing queue after failure
            redisTemplate.opsForList().remove(PROCESSING_QUEUE, 1, jobId);
        }
    }
}
