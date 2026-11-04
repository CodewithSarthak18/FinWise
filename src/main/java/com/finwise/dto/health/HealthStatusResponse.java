package com.finwise.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusResponse {
    private String status;
    private String application;
    private String version;
    private String environment;
    private Instant timestamp;
    private Map<String, Object> components;
}
