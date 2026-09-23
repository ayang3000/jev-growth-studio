package com.example.jevseo.service;

import com.example.jevseo.ai.ContentGenerator;
import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.JevContentReview;
import com.example.jevseo.domain.OptimizationReport;
import com.example.jevseo.domain.OptimizationRequest;
import com.example.jevseo.jev.JevDecisionClient;
import com.example.jevseo.repository.OptimizationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OptimizationService {

    private final JevDecisionClient jevDecisionClient;
    private final ContentGenerator contentGenerator;
    private final OptimizationRepository repository;

    public OptimizationService(JevDecisionClient jevDecisionClient, ContentGenerator contentGenerator,
            OptimizationRepository repository) {
        this.jevDecisionClient = jevDecisionClient;
        this.contentGenerator = contentGenerator;
        this.repository = repository;
    }

    public OptimizationReport optimize(OptimizationRequest request) {
        JevDecision decision = jevDecisionClient.evaluate(request);
        GeneratedContent content = contentGenerator.generate(request, decision);
        JevContentReview contentReview = jevDecisionClient.review(request, content, decision);
        String status = decision.reviewRequired() || decision.readinessScore() < 65 || contentReview.reviewRequired()
                ? "REVIEW_REQUIRED" : "READY_FOR_EXPERIMENT";
        OptimizationReport report = new OptimizationReport(
                UUID.randomUUID(), Instant.now(), request, decision, content,
                contentReview, experiment(request, content), recommendations(decision, contentReview), status);
        repository.save(report);
        return report;
    }

    public OptimizationReport get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Optimization report not found: " + id));
    }

    public List<OptimizationReport> recent(int limit) {
        return repository.findRecent(Math.max(1, Math.min(limit, 100)));
    }

    public void feedback(UUID id, String metric, double baseline, double observed, String notes) {
        get(id);
        repository.saveFeedback(id, metric, baseline, observed, notes);
    }

    public String generatorMode() {
        return contentGenerator.mode();
    }

    private OptimizationReport.ExperimentPlan experiment(OptimizationRequest request, GeneratedContent content) {
        String metric = request.channel().name().equals("ASO") ? "store_listing_conversion_rate" : "organic_ctr";
        return new OptimizationReport.ExperimentPlan(
                "Jev 筛选后的搜索意图与结构化文案将提升目标用户的自然流量转化",
                request.currentContent() == null || request.currentContent().isBlank()
                        ? "现有线上版本" : request.currentContent(),
                content.title(),
                metric,
                request.channel().name().equals("ASO")
                        ? List.of("retention_day_1", "uninstall_rate", "brand_query_share")
                        : List.of("bounce_rate", "conversion_rate", "brand_query_share"),
                14);
    }

    private List<String> recommendations(JevDecision decision, JevContentReview review) {
        List<String> values = new ArrayList<>();
        if (decision.reviewRequired()) {
            values.add("Jev 信号要求人工复核；在品牌负责人确认前不要直接发布。");
        }
        if (decision.brandSafeProbability() < .8) {
            values.add("品牌安全概率不足 0.80，请补充禁用词、语气和合规声明后重跑。");
        }
        if (decision.readinessScore() < 65) {
            values.add("输入证据不足，建议补充真实搜索量、难度、当前排名和转化基线。");
        }
        if (review.groundedProbability() < .85) {
            values.add("生成后事实落地概率不足 0.85，请逐条核验能力、数据与比较性表述。");
        }
        if (review.naturalKeywordProbability() < .75) {
            values.add("关键词自然度不足 0.75，请减少重复并优先保障可读性。");
        }
        values.add("先以 14 天受控实验验证，不要把模型概率当作业务事实。");
        values.add("把实验结果回传到 feedback 接口，用于下一轮关键词和策略校准。");
        return values;
    }
}
