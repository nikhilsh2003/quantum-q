package com.myProject.quantum_q.model;

import java.util.Map;

public record Job(
        String id,
        String status,
        String priority,
        Map<String, Object> payload
) {
}
