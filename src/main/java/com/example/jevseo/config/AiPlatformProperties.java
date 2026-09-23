package com.example.jevseo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "growth-ai")
public record AiPlatformProperties(Jev jev, Generator generator) {

    public record Jev(
            boolean enabled,
            String url,
            String apiKey,
            String model,
            double confidenceThreshold,
            int timeoutSeconds) {
    }

    public record Generator(boolean enabled) {
    }
}
