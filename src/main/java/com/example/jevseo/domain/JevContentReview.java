package com.example.jevseo.domain;

import java.util.Map;

public record JevContentReview(
        String source,
        String publishDecision,
        int intentAlignmentScore,
        double naturalKeywordProbability,
        double brandSafeProbability,
        double groundedProbability,
        double confidence,
        boolean reviewRequired,
        Map<String, Object> rawSignals) {

    public JevContentReview {
        rawSignals = rawSignals == null ? Map.of() : Map.copyOf(rawSignals);
    }
}
