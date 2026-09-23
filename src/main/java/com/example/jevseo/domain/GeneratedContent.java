package com.example.jevseo.domain;

import java.util.List;

public record GeneratedContent(
        String title,
        String subtitle,
        String slug,
        String metaDescription,
        String shortDescription,
        String longDescription,
        List<String> outline,
        List<String> targetKeywords,
        List<String> callsToAction,
        List<String> screenshotCaptions,
        List<String> schemaTypes) {

    public GeneratedContent {
        outline = safe(outline);
        targetKeywords = safe(targetKeywords);
        callsToAction = safe(callsToAction);
        screenshotCaptions = safe(screenshotCaptions);
        schemaTypes = safe(schemaTypes);
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
