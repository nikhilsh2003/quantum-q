package com.myProject.quantum_q.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myProject.quantum_q.dto.JobRequest;
import com.myProject.quantum_q.model.Job;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper; // Spring Boot auto-configures this bean

    public JobService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String enqueueJob(JobRequest jobRequest) {
        try {
            logger.info("Attempting to enqueue a new job with priority: {}", jobRequest.priority());
            String jobId = UUID.randomUUID().toString();
            logger.debug("Generated job ID: {}", jobId);
            Job job = new Job(jobId, "QUEUED", jobRequest.priority(), jobRequest.payload());

            // Serialize Job object to JSON string
            logger.debug("Serializing job object to JSON for job ID: {}", jobId);
            String jobJson = objectMapper.writeValueAsString(job);

            // Push the job to the left of the list (LPUSH)
            String queueName = "queues:" + jobRequest.priority().toLowerCase();
            logger.debug("Pushing job {} to Redis queue: {}", jobId, queueName);
            redisTemplate.opsForList().leftPush(queueName, jobJson);
            logger.info("Successfully enqueued job with ID: {}", jobId);

            return jobId;
        } catch (Exception e) {
            // In a real app, handle this exception properly
            logger.error("Error enqueuing job: {}", e.getMessage(), e);
            throw new RuntimeException("Error enqueuing job", e); // Re-throw for upstream handling
        }
    }
}
