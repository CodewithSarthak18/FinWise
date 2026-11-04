package com.finwise.controller;

import com.finwise.dto.health.HealthStatusResponse;
import com.finwise.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthStatusResponse>> checkHealth() {
        Map<String, Object> components = new LinkedHashMap<>();
        boolean isDatabaseHealthy = false;

        try (Connection connection = dataSource.getConnection()) {
            isDatabaseHealthy = connection.isValid(2);
            components.put("database", Map.of(
                    "status", isDatabaseHealthy ? "UP" : "DOWN",
                    "databaseProductName", connection.getMetaData().getDatabaseProductName(),
                    "databaseProductVersion", connection.getMetaData().getDatabaseProductVersion()
            ));
        } catch (Exception e) {
            components.put("database", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }

        Runtime runtime = Runtime.getRuntime();
        components.put("jvm", Map.of(
                "totalMemoryMB", runtime.totalMemory() / (1024 * 1024),
                "freeMemoryMB", runtime.freeMemory() / (1024 * 1024),
                "maxMemoryMB", runtime.maxMemory() / (1024 * 1024),
                "availableProcessors", runtime.availableProcessors()
        ));

        String overallStatus = isDatabaseHealthy ? "UP" : "DOWN";
        String[] activeProfiles = environment.getActiveProfiles();
        String currentEnv = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";

        HealthStatusResponse response = HealthStatusResponse.builder()
                .status(overallStatus)
                .application("FinWise Personal Finance Backend")
                .version("1.0.0")
                .environment(currentEnv)
                .timestamp(Instant.now())
                .components(components)
                .build();

        HttpStatus httpStatus = isDatabaseHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(ApiResponse.success("Health check completed", response));
    }
}
