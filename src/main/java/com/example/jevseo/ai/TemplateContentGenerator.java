package com.example.jevseo.ai;

import com.example.jevseo.domain.Channel;
import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;

import java.util.Comparator;
import java.util.List;

public final class TemplateContentGenerator implements ContentGenerator {

    @Override
    public GeneratedContent generate(OptimizationRequest request, JevDecision decision) {
        List<String> keywords = decision.keywordVerdicts().values().stream()
                .filter(item -> item.useProbability() >= .5)
                .sorted(Comparator.comparingInt(JevDecision.KeywordVerdict::fitScore).reversed())
                .map(JevDecision.KeywordVerdict::keyword)
                .limit(8)
                .toList();
        String primary = keywords.isEmpty() ? request.productName() : keywords.get(0);
        if (request.channel() == Channel.ASO) {
            return new GeneratedContent(
                    trim(request.productName() + " - " + primary, 30),
                    trim(request.goal(), 30),
                    "",
                    "",
                    trim("面向" + request.audience() + "：" + request.goal(), 80),
                    request.productName() + " 帮助" + request.audience() + "实现：" + request.goal()
                            + "。\n\n核心价值\n• 围绕真实任务快速开始\n• 清晰呈现关键收益\n• 持续通过实验验证效果",
                    List.of(), keywords,
                    List.of("立即下载并开始体验"),
                    List.of("第一眼看懂核心价值", "三步完成关键任务", "为目标用户而设计", "掌握结果与进度", "立即开始体验"),
                    List.of());
        }
        String slug = primary.toLowerCase().trim().replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("(^-|-$)", "");
        return new GeneratedContent(
                trim(primary + "：" + request.goal(), 60),
                "",
                slug,
                trim("了解" + request.productName() + "如何帮助" + request.audience() + "实现" + request.goal() + "。", 155),
                "",
                request.productName() + " 面向" + request.audience() + "，聚焦“" + primary + "”相关需求。"
                        + "本文将从目标、实施步骤和评估指标三个方面给出可执行方案。",
                List.of("用户问题与搜索意图", "解决方案与实施步骤", "常见误区", "效果衡量", "下一步行动"),
                keywords,
                List.of("获取方案", "开始免费评估"),
                List.of(),
                List.of("Article", "BreadcrumbList", "FAQPage"));
    }

    private String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @Override
    public String mode() {
        return "template-fallback";
    }
}
