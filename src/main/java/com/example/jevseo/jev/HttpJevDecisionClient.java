package com.example.jevseo.jev;

import com.example.jevseo.config.AiPlatformProperties;
import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevContentReview;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HttpJevDecisionClient implements JevDecisionClient {

    private static final List<String> SCORE_LEVELS = List.of("very_low", "low", "medium", "high", "very_high");
    private final RestClient restClient;
    private final AiPlatformProperties.Jev properties;

    public HttpJevDecisionClient(RestClient.Builder builder, AiPlatformProperties.Jev properties) {
        this.properties = properties;
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);
        this.restClient = builder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public JevDecision evaluate(OptimizationRequest request) {
        Map<String, Object> questions = new LinkedHashMap<>();
        questions.put("strategy", choice(
                "Choose the best optimization format for this goal and channel.",
                Map.of(
                        "seo_article", "Search-focused long-form article",
                        "seo_landing_page", "Conversion-focused search landing page",
                        "aso_listing", "App store listing metadata and description",
                        "aso_experiment", "App store listing A/B experiment")));
        questions.put("opportunity", score(
                "Score the overall organic growth opportunity using demand, difficulty, rank and audience fit."));
        questions.put("readiness", score(
                "Score how ready the supplied evidence is for generating a publishable optimization."));
        questions.put("brand_safe", noul(
                "Would content following this brief likely comply with every supplied brand rule?"));
        questions.put("needs_human_review", noul(
                "Does this optimization need human review because of uncertainty, brand risk, weak data, or sensitive claims?"));

        for (int i = 0; i < request.keywords().size(); i++) {
            questions.put("kw_" + i + "_intent", choice(
                    "Classify keyword index " + i + " by dominant search intent.",
                    Map.of(
                            "informational", "Learn or solve a problem",
                            "commercial", "Compare or evaluate options",
                            "transactional", "Install, buy, subscribe, or act",
                            "navigational", "Find a known brand, product, or page",
                            "discovery", "Broad exploration without a clear action")));
            questions.put("kw_" + i + "_fit", score(
                    "Score keyword index " + i + " for relevance and attainable organic impact."));
            questions.put("kw_" + i + "_use", noul(
                    "Should keyword index " + i + " be used in the primary optimized content?"));
        }

        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "state", state(request),
                "questions", questions);

        JsonNode response = invoke(body);
        JsonNode result = response.has("data") ? response.path("data") : response;
        JsonNode answers = result.path("answers");
        if (!answers.isObject()) {
            throw new IllegalStateException("Jev response does not contain typed answers");
        }
        return mapDecision(request, answers, result);
    }

    @Override
    public JevContentReview review(OptimizationRequest request, GeneratedContent content, JevDecision decision) {
        Map<String, Object> questions = new LinkedHashMap<>();
        questions.put("intent_alignment", score(
                "Score how closely the generated asset satisfies the audience, goal, channel and selected search intent."));
        questions.put("natural_keyword_use", noul(
                "Are target keywords used naturally without repetition, stuffing, or misleading placement?"));
        questions.put("brand_safe", noul(
                "Does the generated asset comply with every supplied brand rule?"));
        questions.put("grounded", noul(
                "Is the asset free of unsupported statistics, rankings, awards, reviews, capabilities and competitor claims?"));
        questions.put("publish_route", choice(
                "Choose the safest next step for this generated organic-growth asset.",
                Map.of(
                        "experiment", "Safe enough for a controlled experiment after normal editorial approval",
                        "review", "Needs a human editor or brand owner review",
                        "regenerate", "Should be regenerated because quality or evidence is weak",
                        "block", "Must not be used because of serious compliance or grounding risk")));

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("channel", request.channel());
        state.put("product", request.productName());
        state.put("audience", request.audience());
        state.put("goal", request.goal());
        state.put("brand_rules", request.brandRules());
        state.put("strategy", decision.strategy());
        state.put("approved_keywords", decision.keywordVerdicts());
        state.put("generated_content", content);

        JsonNode response = invoke(Map.of(
                "model", properties.model(), "state", state, "questions", questions));
        JsonNode result = response.has("data") ? response.path("data") : response;
        JsonNode answers = result.path("answers");
        if (!answers.isObject()) {
            throw new IllegalStateException("Jev review response does not contain typed answers");
        }
        JsonNode alignment = answers.path("intent_alignment");
        JsonNode natural = answers.path("natural_keyword_use");
        JsonNode brandSafe = answers.path("brand_safe");
        JsonNode grounded = answers.path("grounded");
        JsonNode route = answers.path("publish_route");
        List<Double> confidences = new ArrayList<>();
        collectConfidence(alignment, confidences);
        collectConfidence(route, confidences);
        double confidence = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        String publishDecision = route.path("choice").asText("review");
        double naturalProbability = natural.path("noul").asDouble(.5);
        double brandProbability = brandSafe.path("noul").asDouble(.5);
        double groundedProbability = grounded.path("noul").asDouble(.5);
        boolean reviewRequired = !"experiment".equals(publishDecision)
                || confidence < properties.confidenceThreshold()
                || naturalProbability < .75 || brandProbability < .80 || groundedProbability < .85;
        return new JevContentReview(
                "jev", publishDecision, scoreToPercent(alignment.path("score").asDouble(2)),
                naturalProbability, brandProbability, groundedProbability, confidence, reviewRequired,
                JevDecision.signals("model", result.path("model").asText(properties.model()),
                        "usage", result.path("usage")));
    }

    private JsonNode invoke(Map<String, Object> body) {
        JsonNode response = restClient.post().uri(properties.url()).body(body).retrieve().body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Jev returned an empty response");
        }
        return response;
    }

    private Map<String, Object> state(OptimizationRequest request) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        for (int i = 0; i < request.keywords().size(); i++) {
            OptimizationRequest.KeywordCandidate item = request.keywords().get(i);
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("index", i);
            value.put("keyword", item.keyword());
            value.put("monthly_volume", item.monthlyVolume());
            value.put("difficulty_0_to_100", item.difficulty());
            value.put("current_rank", item.currentRank());
            keywords.add(value);
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("channel", request.channel());
        state.put("product", request.productName());
        state.put("market", request.market());
        state.put("language", request.language());
        state.put("audience", request.audience());
        state.put("goal", request.goal());
        state.put("current_content", request.currentContent());
        state.put("competitors", request.competitors());
        state.put("brand_rules", request.brandRules());
        state.put("keywords", keywords);
        return state;
    }

    private JevDecision mapDecision(OptimizationRequest request, JsonNode answers, JsonNode result) {
        Map<String, JevDecision.KeywordVerdict> verdicts = new LinkedHashMap<>();
        List<Double> confidences = new ArrayList<>();
        for (int i = 0; i < request.keywords().size(); i++) {
            String keyword = request.keywords().get(i).keyword();
            JsonNode intent = answers.path("kw_" + i + "_intent");
            JsonNode fit = answers.path("kw_" + i + "_fit");
            JsonNode use = answers.path("kw_" + i + "_use");
            collectConfidence(intent, confidences);
            collectConfidence(fit, confidences);
            verdicts.put(keyword, new JevDecision.KeywordVerdict(
                    keyword,
                    intent.path("choice").asText("discovery"),
                    scoreToPercent(fit.path("score").asDouble(2)),
                    use.path("noul").asDouble(.5)));
        }
        JsonNode strategy = answers.path("strategy");
        JsonNode opportunity = answers.path("opportunity");
        JsonNode readiness = answers.path("readiness");
        JsonNode brandSafe = answers.path("brand_safe");
        JsonNode humanReview = answers.path("needs_human_review");
        collectConfidence(strategy, confidences);
        collectConfidence(opportunity, confidences);
        collectConfidence(readiness, confidences);
        double confidence = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double brandSafeProbability = brandSafe.path("noul").asDouble(.5);
        double humanReviewProbability = humanReview.path("noul").asDouble(.5);
        boolean reviewRequired = confidence < properties.confidenceThreshold()
                || brandSafeProbability < .80
                || humanReviewProbability >= .50;
        return new JevDecision(
                "jev",
                strategy.path("choice").asText(request.channel() == com.example.jevseo.domain.Channel.ASO
                        ? "aso_listing" : "seo_article"),
                scoreToPercent(opportunity.path("score").asDouble(2)),
                scoreToPercent(readiness.path("score").asDouble(2)),
                brandSafeProbability,
                humanReviewProbability,
                confidence,
                reviewRequired,
                verdicts,
                JevDecision.signals("model", result.path("model").asText(properties.model()),
                        "usage", result.path("usage")));
    }

    private void collectConfidence(JsonNode answer, List<Double> confidences) {
        if (answer.has("confidence")) {
            confidences.add(answer.path("confidence").asDouble());
        }
    }

    private int scoreToPercent(double zeroBasedScore) {
        return (int) Math.round(Math.max(0, Math.min(SCORE_LEVELS.size() - 1, zeroBasedScore))
                * 100 / (SCORE_LEVELS.size() - 1));
    }

    private Map<String, Object> choice(String instructions, Map<String, String> criteria) {
        return Map.of("type", "choice", "instructions", instructions, "criteria", criteria);
    }

    private Map<String, Object> score(String instructions) {
        return Map.of("type", "score", "instructions", instructions, "criteria", SCORE_LEVELS);
    }

    private Map<String, Object> noul(String instructions) {
        return Map.of("type", "noul", "instructions", instructions,
                "criteria", Map.of("true", "Yes", "false", "No"));
    }
}
