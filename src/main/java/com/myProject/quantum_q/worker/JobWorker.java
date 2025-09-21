package com.myProject.quantum_q.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myProject.quantum_q.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JobWorker {

    private static final String QUEUE_NAME = "queues:normal";
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public JobWorker(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2000) // Poll every 2 seconds
    public void processJob() {
        // Pop a job from the right of the list (RPOP)
        String jobJson = redisTemplate.opsForList().rightPop(QUEUE_NAME);

        if (jobJson != null) {
            try {
                Job job = objectMapper.readValue(jobJson, Job.class);
                log.info("Processing job: {}", job.id());

                // Simulate work
                Thread.sleep(1000);

                log.info("✅ Job {} completed successfully.", job.id());
            } catch (Exception e) {
                log.error("🔥 Failed to process job.", e);
            }
        }
    }
}
