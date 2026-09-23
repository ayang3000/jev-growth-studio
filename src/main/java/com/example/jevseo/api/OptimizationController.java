package com.example.jevseo.api;

import com.example.jevseo.config.AiPlatformProperties;
import com.example.jevseo.domain.OptimizationReport;
import com.example.jevseo.domain.OptimizationRequest;
import com.example.jevseo.service.OptimizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class OptimizationController {

    private final OptimizationService service;
    private final AiPlatformProperties properties;

    public OptimizationController(OptimizationService service, AiPlatformProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping("/optimizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OptimizationReport optimize(@Valid @RequestBody OptimizationRequest request) {
        return service.optimize(request);
    }

    @GetMapping("/optimizations/{id}")
    public OptimizationReport get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/optimizations")
    public List<OptimizationReport> recent(@RequestParam(defaultValue = "20") int limit) {
        return service.recent(limit);
    }

    @PostMapping("/optimizations/{id}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@PathVariable UUID id, @Valid @RequestBody FeedbackRequest feedback) {
        service.feedback(id, feedback.metric(), feedback.baseline(), feedback.observed(), feedback.notes());
    }

    @GetMapping("/system/status")
    public Map<String, Object> status() {
        return Map.of(
                "jdk", Runtime.version().feature(),
                "jev", properties.jev().enabled() && properties.jev().apiKey() != null
                        && !properties.jev().apiKey().isBlank() ? "remote" : "heuristic-fallback",
                "generator", service.generatorMode(),
                "humanReviewThreshold", properties.jev().confidenceThreshold());
    }

    public record FeedbackRequest(
            @NotBlank String metric,
            double baseline,
            double observed,
            String notes) {
    }
}
