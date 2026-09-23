package com.example.jevseo;

import com.example.jevseo.domain.Channel;
import com.example.jevseo.domain.OptimizationReport;
import com.example.jevseo.domain.OptimizationRequest;
import com.example.jevseo.service.OptimizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OptimizationApiTest {

    @Autowired
    OptimizationService service;

    @Test
    void createsAndPersistsSeoOptimizationWithoutExternalKeys() {
        OptimizationRequest request = new OptimizationRequest(
                Channel.SEO, "FlowNote", "China", "zh-CN",
                "需要整理会议纪要的产品团队", "提升自然搜索注册转化", "",
                List.of(
                        new OptimizationRequest.KeywordCandidate("AI会议纪要", 12000, 42d, 18),
                        new OptimizationRequest.KeywordCandidate("会议纪要工具", 8000, 35d, 26)),
                List.of(), List.of("不得承诺百分之百准确"));

        OptimizationReport report = service.optimize(request);

        assertThat(report.id()).isNotNull();
        assertThat(report.decision().source()).isEqualTo("heuristic-fallback");
        assertThat(report.content().title()).isNotBlank();
        assertThat(report.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(service.get(report.id()).content().title()).isEqualTo(report.content().title());
    }
}
