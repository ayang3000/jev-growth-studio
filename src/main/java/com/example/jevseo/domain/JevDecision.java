package com.example.jevseo.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record JevDecision(
        String source,
        String strategy,
        int opportunityScore,
        int readinessScore,
        double brandSafeProbability,
        double humanReviewProbability,
        double confidence,
        boolean reviewRequired,
        Map<String, KeywordVerdict> keywordVerdicts,
        Map<String, Object> rawSignals) {

    public JevDecision {
        keywordVerdicts = keywordVerdicts == null ? Map.of() : Map.copyOf(keywordVerdicts);
        rawSignals = rawSignals == null ? Map.of() : Map.copyOf(rawSignals);
    }

    public record KeywordVerdict(String keyword, String intent, int fitScore, double useProbability) {
    }

    public static Map<String, Object> signals(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return values;
    }
}
