package com.myProject.quantum_q.dto;

import java.util.Map;

public record JobRequest(String priority, Map<String, Object> payload) {
}
