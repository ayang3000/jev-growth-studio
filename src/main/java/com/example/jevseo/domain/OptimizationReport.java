package com.example.jevseo.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OptimizationReport(
        UUID id,
        Instant createdAt,
        OptimizationRequest request,
        JevDecision decision,
        GeneratedContent content,
        JevContentReview contentReview,
        ExperimentPlan experiment,
        List<String> recommendations,
        String status) {

    public record ExperimentPlan(
            String hypothesis,
            String control,
            String variant,
            String primaryMetric,
            List<String> guardrailMetrics,
            int minimumDays) {
    }
}
