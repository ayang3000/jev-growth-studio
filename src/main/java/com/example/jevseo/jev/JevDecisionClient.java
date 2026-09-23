package com.example.jevseo.jev;

import com.example.jevseo.domain.GeneratedContent;
import com.example.jevseo.domain.JevContentReview;
import com.example.jevseo.domain.JevDecision;
import com.example.jevseo.domain.OptimizationRequest;

public interface JevDecisionClient {

    JevDecision evaluate(OptimizationRequest request);

    JevContentReview review(OptimizationRequest request, GeneratedContent content, JevDecision decision);
}
