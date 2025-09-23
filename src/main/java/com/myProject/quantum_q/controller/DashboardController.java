package com.myProject.quantum_q.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final RedisTemplate<String, String> redisTemplate;

    public DashboardController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // This method serves the main HTML page
    @GetMapping("/")
    public String dashboard() {
        return "index"; // This corresponds to src/main/resources/templates/index.html
    }

    // This is a REST endpoint that returns JSON data for the UI to fetch
    @GetMapping("/api/v1/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = Map.of(
                "high", redisTemplate.opsForList().size("queues:high"),
                "normal", redisTemplate.opsForList().size("queues:normal"),
                "low", redisTemplate.opsForList().size("queues:low"),
                "dlq", redisTemplate.opsForList().size("queues:dlq"),
                "processing", redisTemplate.opsForList().size("queues:processing")
        );
        return ResponseEntity.ok(stats);
    }

    // This endpoint returns the 10 most recently processed jobs
    @GetMapping("/api/v1/jobs/recent")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRecentJobs() {
        // For this demo, we'll just pull the latest 10 failed jobs from the DLQ
        // A more robust implementation might use a dedicated "completed" list
        List<String> failedJobPayloads = redisTemplate.opsForList().range("queues:dlq", 0, 9);

        // This is a simplified example. In a real app, you'd have a better way to track completed jobs.
        // For now, we'll simulate a list of completed jobs.
        List<Map<String, Object>> recentJobs = failedJobPayloads.stream()
                .map(payload -> Map.of("id", (Object)("failed-" + payload.hashCode()), "status", (Object)"FAILED"))
                .collect(Collectors.toList());

        // Let's add a dummy completed job for visuals
        if (recentJobs.isEmpty()) {
            recentJobs.add(Map.of("id", (Object)"dummy-12345", "status", (Object)"COMPLETED"));
        }

        return ResponseEntity.ok(recentJobs);
    }
}
