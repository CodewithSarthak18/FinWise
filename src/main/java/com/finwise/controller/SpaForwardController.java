package com.finwise.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Controller to support React Single Page Application (SPA) routing.
 * Forwards client-side application routes to index.html or provides a clean API status.
 */
@Controller
public class SpaForwardController {

    private final boolean hasStaticIndex = new ClassPathResource("static/index.html").exists();

    @GetMapping(value = "/")
    public Object handleRoot() {
        if (hasStaticIndex) {
            return "forward:/index.html";
        }
        return ResponseEntity.ok(Map.of(
                "application", "FinWise Personal Finance Backend",
                "status", "UP",
                "message", "Welcome to FinWise API. Health check available at /api/v1/health",
                "version", "1.0.0"
        ));
    }

    @GetMapping(value = {"/api/v1", "/api/v1/"})
    public ResponseEntity<Map<String, Object>> handleApiRoot() {
        return ResponseEntity.ok(Map.of(
                "application", "FinWise Personal Finance Backend API",
                "version", "1.0.0",
                "status", "UP",
                "healthCheckUrl", "/api/v1/health",
                "documentation", "/swagger-ui.html"
        ));
    }

    @GetMapping(value = {
            "/login",
            "/register",
            "/dashboard",
            "/transactions",
            "/budgets",
            "/goals",
            "/recurring",
            "/analytics",
            "/calendar",
            "/settings",
            "/reports",
            "/accounts",
            "/subscriptions",
            "/what-if"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
