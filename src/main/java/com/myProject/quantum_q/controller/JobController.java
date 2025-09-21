package com.myProject.quantum_q.controller;

import com.myProject.quantum_q.dto.JobRequest;
import com.myProject.quantum_q.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<Map<String, String>> submitJob(@RequestBody JobRequest jobRequest) {
        String jobId = jobService.enqueueJob(jobRequest);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }
}
