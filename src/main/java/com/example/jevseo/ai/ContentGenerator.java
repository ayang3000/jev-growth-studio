package com.example.jevseo.ai;

import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;

public interface ContentGenerator {

    GeneratedContent generate(OptimizationRequest request, JevDecision decision);

    String mode();
}
