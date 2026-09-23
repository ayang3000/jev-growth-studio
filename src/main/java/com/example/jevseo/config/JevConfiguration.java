package com.example.jevseo.config;

import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevContentReview;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;
import com.example.jevseo.jev.HeuristicJevDecisionClient;
import com.example.jevseo.jev.HttpJevDecisionClient;
import com.example.jevseo.jev.JevDecisionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class JevConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JevConfiguration.class);

    @Bean
    JevDecisionClient jevDecisionClient(RestClient.Builder builder, AiPlatformProperties properties) {
        HeuristicJevDecisionClient fallback = new HeuristicJevDecisionClient();
        AiPlatformProperties.Jev jev = properties.jev();
        if (!jev.enabled() || !StringUtils.hasText(jev.apiKey())) {
            return fallback;
        }
        HttpJevDecisionClient remote = new HttpJevDecisionClient(builder, jev);
        return new JevDecisionClient() {
            @Override
            public JevDecision evaluate(OptimizationRequest request) {
                try {
                    return remote.evaluate(request);
                }
                catch (RuntimeException exception) {
                    log.warn("Jev evaluation failed; using review-only fallback: {}", exception.getMessage());
                    return fallback.evaluate(request);
                }
            }

            @Override
            public JevContentReview review(
                    OptimizationRequest request, GeneratedContent content, JevDecision decision) {
                try {
                    return remote.review(request, content, decision);
                }
                catch (RuntimeException exception) {
                    log.warn("Jev content review failed; requiring human review: {}", exception.getMessage());
                    return fallback.review(request, content, decision);
                }
            }
        };
    }
}
