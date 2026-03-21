package com.llmproxy.llm_governance_proxy.policy.service;

import com.llmproxy.llm_governance_proxy.common.dto.DetectionResult;
import com.llmproxy.llm_governance_proxy.common.dto.PolicyDecision;
import com.llmproxy.llm_governance_proxy.common.enums.Action;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    public PolicyDecision evaluate(DetectionResult result, String prompt) {

        PolicyDecision decision = new PolicyDecision();

        // BLOCK if credit card detected
        if (result.getDetectedTypes().contains("CREDIT_CARD")) {
            decision.setAction(Action.BLOCK.name());
            decision.setModifiedPrompt(null);

        }
        // MASK email
        else if (result.getDetectedTypes().contains("EMAIL")) {
            decision.setAction(Action.MASK.name());
            decision.setModifiedPrompt(
                    prompt.replaceAll(
                            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
                            "[REDACTED_EMAIL]"
                    )
            );

        }
        // MASK secrets (password, tokens, etc.)
        else if (result.getDetectedTypes().contains("SECRET")) {
            decision.setAction(Action.MASK.name());
            decision.setModifiedPrompt("[REDACTED_SECRET]");

        }
        // MASK phone numbers (future ready)
        else if (result.getDetectedTypes().contains("PHONE")) {
            decision.setAction(Action.MASK.name());
            decision.setModifiedPrompt("[REDACTED_PHONE]");

        }
        // ALLOW safe input
        else {
            decision.setAction(Action.ALLOW.name());
            decision.setModifiedPrompt(prompt);
        }

        return decision;
    }
}