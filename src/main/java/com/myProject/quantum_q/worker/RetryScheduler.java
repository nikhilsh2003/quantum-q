package com.myProject.quantum_q.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final String RETRY_SCHEDULE = "schedule:retries";
    private final RedisTemplate<String, String> redisTemplate;

    public RetryScheduler(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void requeueDueJobs() {
        Set<String> dueJobs = redisTemplate.opsForZSet().rangeByScore(RETRY_SCHEDULE, 0, System.currentTimeMillis());
        if (dueJobs != null && !dueJobs.isEmpty()) {
            for(String jobId : dueJobs) {
                if(redisTemplate.opsForZSet().remove(RETRY_SCHEDULE, jobId) > 0) {
                    String jobKey = "job:" + jobId;
                    String priority = (String) redisTemplate.opsForHash().get(jobKey, "priority");
                    String queueName = "queues:" + priority.toLowerCase();
                    redisTemplate.opsForList().leftPush(queueName, jobId);
                    log.info("Re-queued job {} for processing.", jobId);
                }
            }
        }
    }

}
