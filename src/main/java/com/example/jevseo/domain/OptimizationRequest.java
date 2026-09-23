package com.example.jevseo.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OptimizationRequest(
        @NotNull Channel channel,
        @NotBlank @Size(max = 120) String productName,
        @NotBlank @Size(max = 80) String market,
        @NotBlank @Size(max = 30) String language,
        @NotBlank @Size(max = 800) String audience,
        @NotBlank @Size(max = 1200) String goal,
        @Size(max = 8000) String currentContent,
        @NotEmpty @Size(max = 20) List<@Valid KeywordCandidate> keywords,
        @Size(max = 20) List<@Size(max = 120) String> competitors,
        @Size(max = 20) List<@Size(max = 300) String> brandRules) {

    public record KeywordCandidate(
            @NotBlank @Size(max = 100) String keyword,
            Integer monthlyVolume,
            Double difficulty,
            Integer currentRank) {
    }
}
