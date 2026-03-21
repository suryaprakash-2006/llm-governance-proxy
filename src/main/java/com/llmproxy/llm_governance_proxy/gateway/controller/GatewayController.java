package com.llmproxy.llm_governance_proxy.gateway.controller;

import com.llmproxy.llm_governance_proxy.common.dto.DetectionResult;
import com.llmproxy.llm_governance_proxy.common.dto.PolicyDecision;
import com.llmproxy.llm_governance_proxy.common.dto.PromptRequest;
import com.llmproxy.llm_governance_proxy.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/llm")
public class GatewayController {

    @Autowired
    private PolicyService policyService;

    @PostMapping
    public PolicyDecision handleRequest(@RequestBody PromptRequest request) {

        // 🔹 Call detection layer (currently simulated)
        DetectionResult detectionResult = callDetectionService(request);

        // 🔹 Logging (important for demo)
        System.out.println("Incoming prompt: " + request.getPrompt());
        System.out.println("Detected types: " + detectionResult.getDetectedTypes());

        // 🔹 Apply policy
        PolicyDecision decision = policyService.evaluate(detectionResult, request.getPrompt());

        System.out.println("Policy action: " + decision.getAction());

        return decision;
    }

    // 🔹 THIS replaces mockDetection (integration-ready method)
    private DetectionResult callDetectionService(PromptRequest request) {

        // 🚨 TEMP simulation (will be replaced with real API call)
        DetectionResult result = new DetectionResult();
        ArrayList<String> detected = new ArrayList<>();

        String prompt = request.getPrompt().toLowerCase();

        // EMAIL detection
        if (prompt.contains("@")) {
            detected.add("EMAIL");
        }

        // SECRET detection
        if (prompt.contains("password")) {
            detected.add("SECRET");
        }

        // CREDIT CARD detection (improved)
        if (prompt.matches(".*\\d{16}.*")) {
            detected.add("CREDIT_CARD");
        }

        result.setDetectedTypes(detected);
        return result;
    }
}