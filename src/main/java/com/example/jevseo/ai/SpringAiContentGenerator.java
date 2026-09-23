package com.example.jevseo.ai;

import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class SpringAiContentGenerator implements ContentGenerator {

    private final ChatClient chatClient;

    public SpringAiContentGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public GeneratedContent generate(OptimizationRequest request, JevDecision decision) {
        String approvedKeywords = decision.keywordVerdicts().values().stream()
                .filter(item -> item.useProbability() >= .5)
                .sorted(Comparator.comparingInt(JevDecision.KeywordVerdict::fitScore).reversed())
                .map(item -> item.keyword() + " [intent=" + item.intent() + ", fit=" + item.fitScore() + "]")
                .collect(Collectors.joining(", "));
        String brandRules = request.brandRules() == null ? "none supplied"
                : String.join("; ", request.brandRules());
        String prompt = """
                Create one production-quality organic growth asset from the brief below.
                Do not invent rankings, reviews, awards, statistics, product capabilities, or competitor facts.
                Follow every brand rule. Use keywords naturally; never keyword-stuff.
                For SEO: title <= 60 chars, meta description <= 155 chars, useful outline, readable long description,
                canonical slug, relevant schema types and conversion calls-to-action.
                For ASO: store title <= 30 chars, subtitle <= 30 chars, short description <= 80 chars,
                benefit-led long description, keyword set, and five screenshot captions. Respect locale conventions.
                Leave inapplicable fields as empty strings or empty lists.

                Channel: %s
                Jev strategy: %s
                Product: %s
                Market and language: %s / %s
                Audience: %s
                Goal: %s
                Approved keyword candidates: %s
                Brand rules: %s
                Current content to improve: %s
                """.formatted(
                request.channel(), decision.strategy(), request.productName(), request.market(), request.language(),
                request.audience(), request.goal(), approvedKeywords, brandRules,
                request.currentContent() == null ? "none" : request.currentContent());

        return chatClient.prompt()
                .system("You are a senior technical SEO and app-store optimization editor. Return only grounded, compliant content.")
                .user(prompt)
                .call()
                .entity(GeneratedContent.class, spec -> spec.validateSchema());
    }

    @Override
    public String mode() {
        return "spring-ai";
    }
}
