package com.example.jevseo.jev;

import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevContentReview;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HeuristicJevDecisionClient implements JevDecisionClient {

    @Override
    public JevDecision evaluate(OptimizationRequest request) {
        Map<String, JevDecision.KeywordVerdict> verdicts = new LinkedHashMap<>();
        int scoreSum = 0;
        for (OptimizationRequest.KeywordCandidate candidate : request.keywords()) {
            int volumeScore = candidate.monthlyVolume() == null
                    ? 50 : Math.min(100, (int) Math.round(Math.log10(candidate.monthlyVolume() + 10) * 23));
            int difficultyPenalty = candidate.difficulty() == null
                    ? 25 : (int) Math.round(Math.max(0, Math.min(100, candidate.difficulty())) * .42);
            int rankBoost = candidate.currentRank() != null && candidate.currentRank() <= 30 ? 12 : 0;
            int fit = Math.max(15, Math.min(95, volumeScore - difficultyPenalty + 32 + rankBoost));
            double useProbability = Math.min(.95, Math.max(.15, fit / 100.0));
            String intent = inferIntent(candidate.keyword());
            verdicts.put(candidate.keyword(),
                    new JevDecision.KeywordVerdict(candidate.keyword(), intent, fit, useProbability));
            scoreSum += fit;
        }
        int opportunity = scoreSum / Math.max(1, verdicts.size());
        String strategy = request.channel().name().equals("ASO") ? "aso_listing" : "seo_article";
        boolean hasRules = request.brandRules() != null && !request.brandRules().isEmpty();
        double humanReview = hasRules ? .35 : .58;
        return new JevDecision(
                "heuristic-fallback",
                strategy,
                opportunity,
                Math.max(45, opportunity - 5),
                hasRules ? .84 : .72,
                humanReview,
                .55,
                true,
                verdicts,
                JevDecision.signals(
                        "reason", "Jev 未启用或不可用，结果必须人工复核",
                        "mode", "demo"));
    }

    @Override
    public JevContentReview review(OptimizationRequest request, GeneratedContent content, JevDecision decision) {
        return new JevContentReview(
                "heuristic-fallback", "review", 55, .55, .65, .55, .5, true,
                Map.of("reason", "Jev 未启用或不可用，无法自动批准生成内容"));
    }

    private String inferIntent(String keyword) {
        String value = keyword.toLowerCase(Locale.ROOT);
        if (value.matches(".*(buy|price|pricing|download|购买|价格|下载).*")) {
            return "transactional";
        }
        if (value.matches(".*(best|vs|compare|review|推荐|对比|评测).*")) {
            return "commercial";
        }
        if (value.matches(".*(how|what|guide|教程|如何|什么|指南).*")) {
            return "informational";
        }
        return "discovery";
    }
}
